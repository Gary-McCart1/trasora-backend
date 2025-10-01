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

        System.out.println("🔑 Original APNS key length: " + apnKeyEnvVar.length());
        System.out.println("🔑 KeyId: " + keyId + ", TeamId: " + teamId);
        System.out.println("🔑 Environment: " + environment);

        // Handle both actual newlines and escaped \n from Heroku
        String formattedKey = apnKeyEnvVar.trim();

        // If the key doesn't have actual newlines, try replacing escaped ones
        if (!formattedKey.contains("\n")) {
            formattedKey = formattedKey.replace("\\n", "\n");
            System.out.println("🔄 Replaced escaped newlines");
        } else {
            System.out.println("✓ Key already has actual newlines");
        }

        // Verify the key starts and ends correctly
        if (!formattedKey.startsWith("-----BEGIN PRIVATE KEY-----")) {
            throw new IllegalArgumentException("Key must start with -----BEGIN PRIVATE KEY-----");
        }
        if (!formattedKey.endsWith("-----END PRIVATE KEY-----")) {
            throw new IllegalArgumentException("Key must end with -----END PRIVATE KEY-----");
        }

        System.out.println("🔑 Formatted key preview:\n" + formattedKey.substring(0, Math.min(80, formattedKey.length())) + "...");
        System.out.println("🔑 Key ends with: ..." + formattedKey.substring(Math.max(0, formattedKey.length() - 30)));

        ApnsClientBuilder builder = new ApnsClientBuilder()
                .setSigningKey(ApnsSigningKey.loadFromInputStream(
                        new ByteArrayInputStream(formattedKey.getBytes(StandardCharsets.UTF_8)),
                        teamId,
                        keyId
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
            String deviceToken,
            String title,
            String body,
            String imageUrl,  // optional album art / profile pic
            String url        // optional link to post or profile
    ) {
        System.out.println("📲 Preparing rich push for device token: " + deviceToken);

        SimpleApnsPayloadBuilder payloadBuilder = new SimpleApnsPayloadBuilder();
        payloadBuilder.setAlertTitle(title);
        payloadBuilder.setAlertBody(body);
        payloadBuilder.setSound("default");
        payloadBuilder.setMutableContent(true);          // allow media attachments
        payloadBuilder.setThreadId("trasora-notifications"); // group notifications

        if (imageUrl != null) {
            payloadBuilder.addCustomProperty("image", imageUrl); // frontend can display
        }
        if (url != null) {
            payloadBuilder.addCustomProperty("url", url); // frontend opens link
        }

        String payload = payloadBuilder.build();
        System.out.println("📝 Payload: " + payload);

        String token = TokenUtil.sanitizeTokenString(deviceToken);
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