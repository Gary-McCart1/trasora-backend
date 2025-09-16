package com.example.blog.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Service
public class AppleMusicTokenService {

    private static final Logger logger = LoggerFactory.getLogger(AppleMusicTokenService.class);

    @Value("${apple.music.key}")
    private String privateKeyContent;

    @Value("${apple.music.key-id}")
    private String keyId;

    @Value("${apple.music.team-id}")
    private String teamId;

    @Value("${apple.music.token-expiration:15777000}") // default 6 months
    private long tokenExpirationSeconds;

    private ECPrivateKey ecPrivateKey;

    @PostConstruct
    private void init() {
        try {
            logger.info("Initializing Apple Music key from environment variable...");

            // Remove BEGIN/END lines
            String cleanedKey = privateKeyContent
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", ""); // remove all spaces and line breaks

            byte[] keyBytes = Base64.getDecoder().decode(cleanedKey);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            ecPrivateKey = (ECPrivateKey) keyFactory.generatePrivate(keySpec);

            logger.info("ECPrivateKey loaded successfully from env variable");
        } catch (Exception e) {
            logger.error("Failed to load Apple Music private key", e);
            throw new RuntimeException("Failed to load Apple Music private key", e);
        }
    }


    public String generateDeveloperToken() {
        try {
            logger.info("Generating Apple Music developer token...");
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
