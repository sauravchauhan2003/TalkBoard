package com.example.TalkBoard.Authentication;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JWTUtil {

    private final String SECRET_KEY = "MysecretjsdhfkushakefmfbskjhfdkjhaebdfkuahdnabdhbsalkDNJjhsbfjkawndkhsbfunadhksbfhajsbhsfnkdJnbhsdbfjansjfinsufbajnfhsbfuanfhjsbghkey"; // Replace with a secure key
    private final long EXPIRATION_TIME = 24 * 60 * 60 * 1000*10;

    // Generate JWT for a user
    public String generateToken(MyUser user) {
        return Jwts.builder()
                .setSubject(Long.toString(user.getId()))
                .claim("username", user.getUsername())
                .claim("email", user.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

    // Validate and parse JWT, return Claims
    public Claims extractAllClaims(String token) throws ExpiredJwtException, MalformedJwtException, SignatureException {
        return Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(token)
                .getBody();
    }

    // Extract specific user data
    public String extractUsername(String token) {
        return extractAllClaims(token).get("username", String.class);
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).get("email", String.class);
    }

    public long extractUserId(String token) {
        return Long.parseLong(extractAllClaims(token).getSubject());
    }

    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (JwtException e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }
}
