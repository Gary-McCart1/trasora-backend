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
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
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

        System.out.println("🔑 Original APNS key length: " + apnKeyEnvVar.length());
        System.out.println("🔑 KeyId: " + keyId + ", TeamId: " + teamId);

        // Strip BEGIN/END lines and remove whitespace (Apple Music style)
        String cleanedKey = apnKeyEnvVar
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] keyBytes = Base64.getDecoder().decode(cleanedKey);
        ECPrivateKey ecPrivateKey = (ECPrivateKey) KeyFactory.getInstance("EC")
                .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));

        // Build APNs client
        ApnsClientBuilder builder = new ApnsClientBuilder()
                .setSigningKey(ApnsSigningKey.loadFromInputStream(
                        new ByteArrayInputStream(keyBytes),
                        keyId,
                        teamId
                ));

        if ("sandbox".equalsIgnoreCase(environment)) {
            builder.setApnsServer(ApnsClientBuilder.DEVELOPMENT_APNS_HOST);
            System.out.println("🌱 Using Sandbox APNs server");
        } else {
            builder.setApnsServer(ApnsClientBuilder.PRODUCTION_APNS_HOST);
            System.out.println("🏭 Using Production APNs server");
        }

        this.apnsClient = builder.build();
        System.out.println("✅ APNs client initialized successfully");
    }

    public CompletableFuture<PushNotificationResponse<SimpleApnsPushNotification>> sendPush(
            String deviceToken, String title, String body
    ) {
        System.out.println("📲 Preparing push for device token: " + deviceToken);

        SimpleApnsPayloadBuilder payloadBuilder = new SimpleApnsPayloadBuilder();
        payloadBuilder.setAlertTitle(title);
        payloadBuilder.setAlertBody(body);
        payloadBuilder.setSound("default");

        String payload = payloadBuilder.build();
        System.out.println("📝 Payload: " + payload);

        String token = TokenUtil.sanitizeTokenString(deviceToken);
        System.out.println("🛡 Sanitized token: " + token);

        SimpleApnsPushNotification pushNotification = new SimpleApnsPushNotification(token, bundleId, payload);

        CompletableFuture<PushNotificationResponse<SimpleApnsPushNotification>> future =
                apnsClient.sendNotification(pushNotification);

        future.whenComplete((response, throwable) -> {
            if (throwable != null) {
                System.err.println("❌ Failed to send push notification:");
                throwable.printStackTrace();
            } else {
                if (response.isAccepted()) {
                    System.out.println("✅ Push accepted by APNs!");
                } else {
                    System.err.println("❌ Push rejected by APNs: " + response.getRejectionReason());
                    response.getTokenInvalidationTimestamp()
                            .ifPresent(ts -> System.err.println("Token invalid as of " + ts));
                }
            }
        });

        return future;
    }
}
