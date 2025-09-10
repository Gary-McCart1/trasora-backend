package com.example.blog.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(AppleMusicTokenService.class);

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
            logger.info("Generating Apple Music developer token...");
            logger.info("Key path: {}", privateKeyPath);
            logger.info("Key ID: {}", keyId);
            logger.info("Team ID: {}", teamId);
            logger.info("Token expiration (seconds): {}", tokenExpirationSeconds);

            // Read the .p8 file content
            String privateKeyContent = new String(Files.readAllBytes(Paths.get(privateKeyPath)))
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            logger.info("Private key read successfully (length: {})", privateKeyContent.length());

            // Decode and create ECPrivateKey
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyContent);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            ECPrivateKey ecPrivateKey = (ECPrivateKey) keyFactory.generatePrivate(keySpec);
            logger.info("ECPrivateKey generated successfully");

            // Create JWT
            Algorithm algorithm = Algorithm.ECDSA256(null, ecPrivateKey);
            Instant now = Instant.now();
            String token = JWT.create()
                    .withIssuer(teamId)
                    .withIssuedAt(Date.from(now))
                    .withExpiresAt(Date.from(now.plusSeconds(tokenExpirationSeconds)))
                    .withKeyId(keyId)
                    .sign(algorithm);

            logger.info("Developer token generated successfully (length: {})", token.length());
            return token;

        } catch (Exception e) {
            logger.error("Failed to generate Apple Music developer token", e);
            throw new RuntimeException("Failed to generate Apple Music developer token", e);
        }
    }
}
