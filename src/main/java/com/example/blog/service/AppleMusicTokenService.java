package com.example.blog.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import jakarta.annotation.PostConstruct;
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
    private String privateKeyPath;

    @Value("${apple.music.key-id}")
    private String keyId;

    @Value("${apple.music.team-id}")
    private String teamId;

    @Value("${apple.music.token-expiration:15777000}")
    private long tokenExpirationSeconds;

    private ECPrivateKey ecPrivateKey;

    @PostConstruct
    private void init() {
        try {
            logger.info("Loading Apple Music private key...");
            String privateKeyContent = new String(Files.readAllBytes(Paths.get(privateKeyPath)))
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");

            byte[] keyBytes = Base64.getDecoder().decode(privateKeyContent);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            ecPrivateKey = (ECPrivateKey) keyFactory.generatePrivate(keySpec);
            logger.info("ECPrivateKey loaded successfully");

        } catch (Exception e) {
            logger.error("Failed to load Apple Music private key", e);
            throw new RuntimeException(e);
        }
    }

    public String generateDeveloperToken() {
        try {
            Algorithm algorithm = Algorithm.ECDSA256(null, ecPrivateKey);
            Instant now = Instant.now();
            return JWT.create()
                    .withIssuer(teamId)
                    .withIssuedAt(Date.from(now))
                    .withExpiresAt(Date.from(now.plusSeconds(tokenExpirationSeconds)))
                    .withKeyId(keyId)
                    .sign(algorithm);

        } catch (Exception e) {
            logger.error("Failed to generate Apple Music developer token", e);
            throw new RuntimeException(e);
        }
    }
}
