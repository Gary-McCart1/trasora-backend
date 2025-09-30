package com.example.blog.service;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.auth.ApnsSigningKey;
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.TokenUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

@Service
public class PushService {

    private final ApnsClient apnsClient;
    private final String bundleId;

    public PushService(
            @Value("${apns.key}") String apnKeyEnvVar,
            @Value("${apns.key-id}") String keyId,
            @Value("${apns.team-id}") String teamId,
            @Value("${apns.bundle-id}") String bundleId,
            @Value("${apns.environment}") String environment
    ) throws Exception {
        this.bundleId = bundleId;

        // Trim and restore line breaks (\n) from single-line Heroku key
        String formattedKey = apnKeyEnvVar.trim().replace("\\n", "\n");

        // Create APNs client using the signing key
        ApnsClientBuilder builder = new ApnsClientBuilder()
                .setSigningKey(ApnsSigningKey.loadFromInputStream(
                        new ByteArrayInputStream(formattedKey.getBytes(StandardCharsets.UTF_8)),
                        keyId,
                        teamId
                ));

        // Set environment
        if ("sandbox".equalsIgnoreCase(environment)) {
            builder.setApnsServer(ApnsClientBuilder.DEVELOPMENT_APNS_HOST);
        } else {
            builder.setApnsServer(ApnsClientBuilder.PRODUCTION_APNS_HOST);
        }

        this.apnsClient = builder.build();
    }

    public CompletableFuture<PushNotificationResponse<SimpleApnsPushNotification>> sendPush(
            String deviceToken, String title, String body
    ) {
        SimpleApnsPayloadBuilder payloadBuilder = new SimpleApnsPayloadBuilder();
        payloadBuilder.setAlertTitle(title);
        payloadBuilder.setAlertBody(body);
        payloadBuilder.setSound("default");

        String payload = payloadBuilder.build();
        String token = TokenUtil.sanitizeTokenString(deviceToken);

        SimpleApnsPushNotification pushNotification = new SimpleApnsPushNotification(token, bundleId, payload);

        return apnsClient.sendNotification(pushNotification);
    }
}
