package com.example.TalkBoard.WebSocketServer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToUser = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> roomParticipants = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> roomAdmins = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToRoom = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String username = (String) session.getAttributes().get("user");
        sessions.put(session.getId(), session);
        sessionToUser.put(session.getId(), username);
        System.out.println("Connected: " + username + " [" + session.getId() + "]");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        JsonNode root = mapper.readTree(message.getPayload());
        String type = root.get("type").asText();
        JsonNode payload = root.get("payload");

        switch (type) {
            case "create-room" -> handleCreateRoom(session);
            case "join-room" -> handleJoinRoom(session, payload);
            case "admit-user" -> handleAdmitUser(session, payload);
            case "kick-user" -> handleKickUser(session, payload);
            case "promote-user" -> handlePromoteUser(session, payload);
            case "mute-user" -> handleMuteUser(session, payload);
            case "sdp" -> forwardMessage(payload, "sdp");
            case "ice" -> forwardMessage(payload, "ice");
        }
    }

    private void handleCreateRoom(WebSocketSession session) throws IOException {
        String roomId = UUID.randomUUID().toString();
        String sessionId = session.getId();

        roomParticipants.put(roomId, new HashSet<>(List.of(sessionId)));
        roomAdmins.put(roomId, new HashSet<>(List.of(sessionId)));
        sessionToRoom.put(sessionId, roomId);

        sendMessage(session, "room-created", Map.of("roomId", roomId, "isAdmin", true));
    }

    private void handleJoinRoom(WebSocketSession session, JsonNode payload) throws IOException {
        String roomId = payload.get("roomId").asText();
        String sessionId = session.getId();
        sessionToRoom.put(sessionId, roomId);

        for (String adminId : roomAdmins.getOrDefault(roomId, Set.of())) {
            sendMessage(sessions.get(adminId), "user-join-request", Map.of(
                    "sessionId", sessionId,
                    "username", sessionToUser.get(sessionId)
            ));
        }
    }

    private void handleAdmitUser(WebSocketSession session, JsonNode payload) throws IOException {
        String targetId = payload.get("sessionId").asText();
        String roomId = sessionToRoom.get(session.getId());

        if (!roomAdmins.getOrDefault(roomId, Set.of()).contains(session.getId())) return;

        roomParticipants.get(roomId).add(targetId);
        sendMessage(sessions.get(targetId), "admitted", Map.of("roomId", roomId, "isAdmin", false));
        broadcast(roomId, "user-joined", Map.of("username", sessionToUser.get(targetId)));
    }

    private void handleKickUser(WebSocketSession session, JsonNode payload) throws IOException {
        String targetId = payload.get("sessionId").asText();
        String roomId = sessionToRoom.get(session.getId());

        if (!roomAdmins.getOrDefault(roomId, Set.of()).contains(session.getId())) return;

        roomParticipants.get(roomId).remove(targetId);
        sessionToRoom.remove(targetId);
        sendMessage(sessions.get(targetId), "kicked", Map.of("reason", "Removed by admin"));
        broadcast(roomId, "user-left", Map.of("username", sessionToUser.get(targetId)));
    }

    private void handlePromoteUser(WebSocketSession session, JsonNode payload) throws IOException {
        String targetId = payload.get("sessionId").asText();
        String roomId = sessionToRoom.get(session.getId());

        if (!roomAdmins.getOrDefault(roomId, Set.of()).contains(session.getId())) return;

        roomAdmins.get(roomId).add(targetId);
        sendMessage(sessions.get(targetId), "promoted", Map.of("isAdmin", true));
    }

    private void handleMuteUser(WebSocketSession session, JsonNode payload) throws IOException {
        String targetId = payload.get("sessionId").asText();
        String media = payload.get("media").asText();
        String roomId = sessionToRoom.get(session.getId());

        if (!roomAdmins.getOrDefault(roomId, Set.of()).contains(session.getId())) return;

        sendMessage(sessions.get(targetId), "muted", Map.of("media", media));
    }

    private void forwardMessage(JsonNode payload, String type) throws IOException {
        String to = payload.get("to").asText();
        WebSocketSession target = sessions.get(to);
        if (target != null && target.isOpen()) {
            sendMessage(target, type, payload);
        }
    }

    private void sendMessage(WebSocketSession session, String type, Object payload) throws IOException {
        Map<String, Object> message = Map.of("type", type, "payload", payload);
        session.sendMessage(new TextMessage(mapper.writeValueAsString(message)));
    }

    private void broadcast(String roomId, String type, Object payload) throws IOException {
        for (String sessionId : roomParticipants.getOrDefault(roomId, Set.of())) {
            WebSocketSession session = sessions.get(sessionId);
            if (session != null && session.isOpen()) {
                sendMessage(session, type, payload);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        sessionToUser.remove(sessionId);

        String roomId = sessionToRoom.remove(sessionId);
        if (roomId != null) {
            // Remove session from roomParticipants
            Set<String> participants = roomParticipants.getOrDefault(roomId, new HashSet<>());
            participants.remove(sessionId);

            // Remove session from roomAdmins
            Set<String> admins = roomAdmins.getOrDefault(roomId, new HashSet<>());
            admins.remove(sessionId);

            // If no admins left, promote the next participant
            if (admins.isEmpty() && !participants.isEmpty()) {
                String newAdminSession = participants.iterator().next();
                admins.add(newAdminSession);
                try {
                    sendMessage(sessions.get(newAdminSession), "promoted", Map.of("isAdmin", true));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // If no participants left, clean up the room
            if (participants.isEmpty()) {
                roomParticipants.remove(roomId);
                roomAdmins.remove(roomId);
                System.out.println("Room deleted: " + roomId);
            } else {
                // Save updated sets
                roomParticipants.put(roomId, participants);
                roomAdmins.put(roomId, admins);
            }
        }
    }

}
