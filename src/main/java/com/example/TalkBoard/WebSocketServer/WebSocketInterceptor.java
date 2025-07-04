package com.example.TalkBoard.WebSocketServer;

import com.example.TalkBoard.Authentication.JWTUtil;
import com.example.TalkBoard.Authentication.MyUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
@Component
public class WebSocketInterceptor implements HandshakeInterceptor {
    @Autowired
    private JWTUtil jwtUtil;
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        String token = getTokenFromRequest(request);
        if (token == null) {
            return false;
        } else {
            if (!jwtUtil.isTokenValid(token)) {
                System.out.println(1);
                return false;
            } else {
                System.out.println(2);
                String user = jwtUtil.extractUsername(token);
                attributes.put("user", user);
                return true;
            }
        }
    }
    private String getTokenFromRequest(ServerHttpRequest request) {
        var headers = request.getHeaders();
        if (headers.containsKey("Authorization")) {
            String authHeader = headers.getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
        }
        // fallback: query parameter
        String uri = request.getURI().toString();
        if (uri.contains("token=")) {
            return uri.substring(uri.indexOf("token=") + 6);
        }
        return null;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
