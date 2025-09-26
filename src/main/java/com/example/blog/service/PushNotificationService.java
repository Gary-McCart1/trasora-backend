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
        if (user.getPushSubscriptionEndpoint() == null) return;

        try {
            PushService pushService = new PushService()
                    .setPublicKey(Utils.loadPublicKey(publicKey))
                    .setPrivateKey(Utils.loadPrivateKey(privateKey));

            // Build JSON payload
            Map<String, String> payloadMap = new HashMap<>();
            payloadMap.put("title", title != null ? title : "Trasora");
            payloadMap.put("body", body != null ? body : "New activity!");
            if (imageUrl != null) {
                payloadMap.put("imageUrl", imageUrl);
            }
            if (url != null) {
                payloadMap.put("url", url);
            }

            String payload = objectMapper.writeValueAsString(payloadMap);

            Notification notification = new Notification(
                    user.getPushSubscriptionEndpoint(),
                    user.getPushSubscriptionKeysP256dh(),
                    user.getPushSubscriptionKeysAuth(),
                    payload
            );

            pushService.send(notification);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendToAllUsers(String title, String body, String imageUrl, String url) {
        List<AppUser> users = userRepository.findAll();
        for (AppUser user : users) {
            sendPushNotification(user, title, body, imageUrl, url);
        }
    }
}
