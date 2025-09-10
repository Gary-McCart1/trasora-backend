package com.example.blog.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Service
public class AppleMusicTokenService {

    @Value("${apple.music.key-path}")
    private String privateKeyPath; // path to your .p8 file

    @Value("${apple.music.key-id}")
    private String keyId;

    @Value("${apple.music.team-id}")
    private String teamId;

    @Value("${apple.music.token-expiration:15777000}") // default 6 months
    private long tokenExpirationSeconds;

    public String generateDeveloperToken() {
        try {
            // Read the .p8 file content
            String privateKeyContent = new String(Files.readAllBytes(Paths.get(privateKeyPath)))
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", ""); // remove newlines and spaces

            // Decode and create ECPrivateKey
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyContent);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            ECPrivateKey ecPrivateKey = (ECPrivateKey) keyFactory.generatePrivate(keySpec);

            // Create JWT
            Algorithm algorithm = Algorithm.ECDSA256(null, ecPrivateKey);
            Instant now = Instant.now();
            return JWT.create()
                    .withIssuer(teamId)
                    .withIssuedAt(Date.from(now))
                    .withExpiresAt(Date.from(now.plusSeconds(tokenExpirationSeconds)))
                    .withKeyId(keyId)
                    .sign(algorithm);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Apple Music developer token", e);
        }
    }
}
