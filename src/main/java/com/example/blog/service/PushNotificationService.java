package com.example.blog.service;

import com.example.blog.entity.AppUser;
import com.example.blog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${VAPID_PUBLIC_KEY}")
    private String publicKey;

    @Value("${VAPID_PRIVATE_KEY}")
    private String privateKey;

    public void sendPushNotification(AppUser user, String title, String body, String imageUrl, String url) {
        System.out.println("Attempting to send push notification to user: " + user.getUsername());

        if (user.getPushSubscriptionEndpoint() == null ||
                user.getPushSubscriptionKeysP256dh() == null ||
                user.getPushSubscriptionKeysAuth() == null) {
            System.out.println("❌ Push subscription missing for user: " + user.getUsername());
            return;
        }

        try {
            PushService pushService = new PushService()
                    .setPublicKey(Utils.loadPublicKey(publicKey))
                    .setPrivateKey(Utils.loadPrivateKey(privateKey));

            System.out.println("✅ PushService initialized for user: " + user.getUsername());

            // Build JSON payload
            Map<String, String> payloadMap = new HashMap<>();
            payloadMap.put("title", title != null ? title : "Trasora");
            payloadMap.put("body", body != null ? body : "New activity!");
            if (imageUrl != null) payloadMap.put("imageUrl", imageUrl);
            payloadMap.put("url", url != null ? url : "/");

            String payload = objectMapper.writeValueAsString(payloadMap);

            System.out.println("Payload for user " + user.getUsername() + ": " + payload);

            Notification notification = new Notification(
                    user.getPushSubscriptionEndpoint(),
                    user.getPushSubscriptionKeysP256dh(),
                    user.getPushSubscriptionKeysAuth(),
                    payload
            );

            pushService.send(notification);
            System.out.println("✅ Push notification sent successfully to user: " + user.getUsername());

        } catch (Exception e) {
            System.out.println("❌ Failed to send push notification to user: " + user.getUsername());
            e.printStackTrace();
        }
    }

    public void sendToAllUsers(String title, String body, String imageUrl, String url) {
        List<AppUser> users = userRepository.findAll();
        System.out.println("Sending push notification to " + users.size() + " users");
        for (AppUser user : users) {
            sendPushNotification(user, title, body, imageUrl, url);
        }
    }
}
