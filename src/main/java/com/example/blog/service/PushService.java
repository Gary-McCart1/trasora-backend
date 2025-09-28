package com.example.blog.service;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.TokenUtil;
import com.eatthepath.pushy.apns.util.ApnsPayloadBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

@Service
public class PushService {

    private final ApnsClient apnsClient;
    private final String bundleId;

    public PushService(
            @Value("${apns.key}") String apnKeyBase64,
            @Value("${apns.key-id}") String keyId,
            @Value("${apns.team-id}") String teamId,
            @Value("${apns.bundle-id}") String bundleId,
            @Value("${apns.environment}") String environment
    ) throws Exception {
        this.bundleId = bundleId;

        // Decode the .p8 key from Base64
        File tempKeyFile = File.createTempFile("apns", ".p8");
        byte[] keyBytes = Base64.getDecoder().decode(apnKeyBase64);
        Files.write(tempKeyFile.toPath(), keyBytes);

        ApnsClientBuilder builder = new ApnsClientBuilder()
                .setClientCredentials(tempKeyFile, teamId); // <-- use keyId here


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
        // Use the concrete builder
        SimpleApnsPayloadBuilder payloadBuilder = new SimpleApnsPayloadBuilder();
        payloadBuilder.setAlertTitle(title);
        payloadBuilder.setAlertBody(body);
        payloadBuilder.setSound("default");

        String payload = payloadBuilder.build(); // build() is available here

        String token = TokenUtil.sanitizeTokenString(deviceToken);

        SimpleApnsPushNotification pushNotification = new SimpleApnsPushNotification(token, bundleId, payload);

        return apnsClient.sendNotification(pushNotification);
    }
}
