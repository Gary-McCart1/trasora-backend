package com.example.blog.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {
    private final String SECRET = "superdupersecretstringthatnooneknowsbutme";
    private final long ACCESS_TOKEN_EXPIRATION = 1000 * 3600;          // 1 hour
    private final long REFRESH_TOKEN_EXPIRATION = 1000L * 60 * 60 * 24 * 31; // 31 days

    private final Key key = Keys.hmacShaKeyFor(SECRET.getBytes());

    private String generateToken(String username, long expirationMillis, String type) {
        return Jwts.builder()
                .setSubject(username)
                .claim("type", type) // 👈 add type claim
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateAccessToken(String username) {
        return generateToken(username, ACCESS_TOKEN_EXPIRATION, "access");
    }

    public String generateRefreshToken(String username) {
        return generateToken(username, REFRESH_TOKEN_EXPIRATION, "refresh");
    }

    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    public String extractType(String token) {
        return getClaims(token).get("type", String.class);
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token); // will throw if invalid/expired
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public String extractTokenFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // strip "Bearer "
        }
        return null;
    }
}
