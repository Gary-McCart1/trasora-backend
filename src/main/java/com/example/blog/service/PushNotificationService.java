package com.example.blog.service;

import com.example.blog.entity.AppUser;
import com.example.blog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private final UserRepository userRepository;

    @Value("${VAPID_PUBLIC_KEY}")
    private String publicKey;

    @Value("${VAPID_PRIVATE_KEY}")
    private String privateKey;

    public void sendPushNotification(AppUser user, String message) {
        if (user.getPushSubscriptionEndpoint() == null) return;

        try {
            PushService pushService = new PushService()
                    .setPublicKey(Utils.loadPublicKey(publicKey))
                    .setPrivateKey(Utils.loadPrivateKey(privateKey));

            Notification notification = new Notification(
                    user.getPushSubscriptionEndpoint(),
                    user.getPushSubscriptionKeysP256dh(),
                    user.getPushSubscriptionKeysAuth(),
                    message
            );

            pushService.send(notification);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void sendToAllUsers(String message) {
        List<AppUser> users = userRepository.findAll();
        for (AppUser user : users) {
            sendPushNotification(user, message);
        }
    }
}
