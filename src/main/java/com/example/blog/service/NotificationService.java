package com.example.blog.service;

import com.example.blog.dto.NotificationDto;
import com.example.blog.entity.*;
import com.example.blog.repository.NotificationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final PushNotificationService pushNotificationService; // web push (VAPID)
    private final PushService pushService; // APNs
    private final UserService userService; // Dependency we will use to fetch a fresh user object

    public Notification createNotification(
            AppUser recipient,
            AppUser sender,
            NotificationType type,
            Post post,
            Follow follow,
            String trunkName,
            String songTitle,
            String songArtist,
            String albumArtUrl
    ) {
        // Skip notifications to self
        if (recipient.getId().equals(sender.getId())) {
            System.out.println("⚠️ Skipping notification: recipient and sender are the same user: " + recipient.getUsername());
            return null;
        }

        System.out.println("📩 Creating notification: type=" + type + ", recipient=" + recipient.getUsername() + ", sender=" + sender.getUsername());

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setSender(sender);
        notification.setType(type);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        if (type == NotificationType.LIKE || type == NotificationType.COMMENT) {
            notification.setPost(post);
        }

        if ((type == NotificationType.FOLLOW || type == NotificationType.FOLLOW_REQUEST || type == NotificationType.FOLLOW_ACCEPTED) && follow != null) {
            notification.setFollow(follow);
        }

        if (type == NotificationType.BRANCH_ADDED) {
            notification.setTrunkName(trunkName);
            notification.setSongTitle(songTitle);
            notification.setSongArtist(songArtist);
            notification.setAlbumArtUrl(albumArtUrl);
        }

        // Save notification
        Notification saved = notificationRepository.save(notification);
        System.out.println("✅ Notification saved with ID: " + saved.getId());

        // 🚨 FIX: Re-fetch the recipient to ensure the APNs token is loaded from the database.
        // Assumes userService has a method like findById or similar. You may need to create it.
        AppUser pushRecipient = userService.findById(recipient.getId())
                .orElseThrow(() -> new RuntimeException("Recipient user not found for push notification."));

        String title = getNotificationTitle(type, sender);
        String body = getNotificationBody(type, post, songTitle, songArtist);
        String url = getNotificationUrl(type, post != null ? post.getId() : null, trunkName);
        String imageUrl = switch (type) {
            case LIKE, COMMENT -> post != null ? (post.getCustomImageUrl() != null ? post.getCustomImageUrl() : post.getAlbumArtUrl()) : null;
            case BRANCH_ADDED -> albumArtUrl;
            default -> null;
        };

        // 1️⃣ APNs push - USE THE FRESHLY FETCHED pushRecipient
        if (pushRecipient.getApnDeviceToken() != null) {
            // Optional: Add debug log to confirm token is loaded
            System.out.println("DEBUG: APNs Token found: " + pushRecipient.getApnDeviceToken().substring(0, 10) + "...");

            try {
                pushService.sendPush(pushRecipient.getApnDeviceToken(), title, body)
                        .thenAccept(response -> {
                            if (response.isAccepted()) {
                                System.out.println("✅ APNs push sent to " + pushRecipient.getUsername());
                            } else {
                                System.out.println("⚠️ APNs rejected for " + pushRecipient.getUsername() + ": " + response.getRejectionReason());
                            }
                        });
            } catch (Exception e) {
                System.err.println("❌ Error sending APNs push to " + pushRecipient.getUsername());
                e.printStackTrace();
            }
        } else {
            // Use the freshly fetched user for logging
            System.out.println("⚠️ Recipient " + pushRecipient.getUsername() + " has no APNs device token. Skipping iOS push.");
        }

        // 2️⃣ Web push (VAPID) - USE THE FRESHLY FETCHED pushRecipient
        if (pushRecipient.getPushSubscriptionEndpoint() != null) {
            System.out.println("🚀 Sending web push: recipient=" + pushRecipient.getUsername() + ", title=" + title + ", body=" + body + ", url=" + url);
            pushNotificationService.sendPushNotification(pushRecipient, title, body, imageUrl, url);
        } else {
            System.out.println("⚠️ Recipient " + pushRecipient.getUsername() + " has no web push subscription. Skipping web push.");
        }

        return saved;
    }

    // -------------------------
    // Helper methods (UNCHANGED)
    // -------------------------
    private String getNotificationTitle(NotificationType type, AppUser sender) {
        return switch (type) {
            case LIKE -> sender.getUsername() + " liked your post";
            case COMMENT -> sender.getUsername() + " commented on your post";
            case FOLLOW -> sender.getUsername() + " followed you";
            case FOLLOW_REQUEST -> sender.getUsername() + " sent you a follow request";
            case FOLLOW_ACCEPTED -> sender.getUsername() + " accepted your follow request";
            case BRANCH_ADDED -> sender.getUsername() + " added a song to your trunk!";
        };
    }

    private String getNotificationBody(NotificationType type, Post post, String songTitle, String songArtist) {
        return switch (type) {
            case LIKE, COMMENT -> post != null && post.getText() != null ? post.getText() : "";
            case BRANCH_ADDED -> songTitle + " by " + songArtist;
            default -> "";
        };
    }

    private String getNotificationUrl(NotificationType type, Long postId, String trunkName) {
        String frontendBase = System.getenv("FRONTEND_URL");
        if (frontendBase == null) frontendBase = "https://trasora-frontend-web.vercel.app/";

        return switch (type) {
            case LIKE, COMMENT -> postId != null ? frontendBase + "/post/" + postId : frontendBase + "/notifications";
            case BRANCH_ADDED -> trunkName != null ? frontendBase + "/trunk/" + trunkName : frontendBase + "/notifications";
            case FOLLOW, FOLLOW_REQUEST, FOLLOW_ACCEPTED -> frontendBase + "/notifications";
        };
    }

    // =========================
    // Convenience creators (UNCHANGED)
    // =========================
    public Notification createFollowRequestNotification(AppUser recipient, AppUser sender, Follow follow) {
        return createNotification(recipient, sender, NotificationType.FOLLOW_REQUEST, null, follow, null, null, null, null);
    }

    public Notification createFollowAcceptedNotification(AppUser recipient, AppUser sender) {
        return createNotification(recipient, sender, NotificationType.FOLLOW_ACCEPTED, null, null, null, null, null, null);
    }

    public Notification createLikeNotification(AppUser recipient, AppUser sender, Post post) {
        return createNotification(recipient, sender, NotificationType.LIKE, post, null, null, null, null, null);
    }

    public Notification createCommentNotification(AppUser recipient, AppUser sender, Post post) {
        return createNotification(recipient, sender, NotificationType.COMMENT, post, null, null, null, null, null);
    }

    public Notification createBranchNotification(AppUser recipient, AppUser sender, String trunkName, String songTitle, String songArtist, String albumArtUrl) {
        return createNotification(recipient, sender, NotificationType.BRANCH_ADDED, null, null, trunkName, songTitle, songArtist, albumArtUrl);
    }

    // =========================
    // Retrieval / update logic (UNCHANGED)
    // =========================

    public List<Notification> getUnreadNotifications(AppUser recipient) {
        return notificationRepository.findByRecipientAndIsReadFalseOrderByCreatedAtDesc(recipient);
    }

    public List<Notification> getAllNotifications(AppUser recipient) {
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(recipient);
    }

    @Transactional
    public void markAsRead(Long notificationId, AppUser recipient) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        if (!notification.getRecipient().equals(recipient)) throw new RuntimeException("Not authorized");
        if (notification.getType() != NotificationType.FOLLOW_REQUEST) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void markAllAsRead(AppUser recipient) {
        List<Notification> unread = notificationRepository.findByRecipientAndIsReadFalseOrderByCreatedAtDesc(recipient);
        unread.stream()
                .filter(n -> n.getType() != NotificationType.FOLLOW_REQUEST)
                .forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    @Transactional
    public void markFollowRequestAsRead(Long notificationId, AppUser recipient) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        if (!notification.getRecipient().equals(recipient)) throw new RuntimeException("Not authorized");
        if (notification.getType() == NotificationType.FOLLOW_REQUEST) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void markFollowRequestAsReadForUser(AppUser follower, AppUser recipient) {
        List<Notification> notifications = notificationRepository
                .findBySenderAndRecipientAndTypeAndIsReadFalse(follower, recipient, NotificationType.FOLLOW_REQUEST);
        notifications.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(notifications);
    }

    @Transactional
    public void deleteNotificationsForFollow(Follow follow) {
        if (follow == null) return;
        List<Notification> notifications = notificationRepository.findByFollow(follow);
        if (!notifications.isEmpty()) notificationRepository.deleteAll(notifications);
    }

    public List<NotificationDto> toDtoList(List<Notification> notifications) {
        return notifications.stream().map(n -> {
            NotificationDto dto = new NotificationDto();
            dto.setId(n.getId());
            dto.setType(n.getType().name());
            dto.setRead(n.isRead());
            dto.setSenderUsername(n.getSender().getUsername());
            dto.setRecipientUsername(n.getRecipient().getUsername());
            dto.setFollowId(n.getFollow() != null ? n.getFollow().getId() : null);
            dto.setCreatedAt(n.getCreatedAt());
            dto.setTrunkName(n.getTrunkName());
            dto.setSongTitle(n.getSongTitle());
            dto.setSongArtist(n.getSongArtist());
            dto.setAlbumArtUrl(n.getAlbumArtUrl());
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void markFollowRequestAsReadForFollowId(Long followId) {
        List<Notification> notifications = notificationRepository.findAllByFollow_Id(followId);
        notifications.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(notifications);
    }

    public void markAllExceptFollowRequestsAsRead(AppUser user) {
        List<Notification> unread = notificationRepository.findByRecipientAndIsReadFalseOrderByCreatedAtDesc(user);
        unread.stream()
                .filter(n -> n.getType() != NotificationType.FOLLOW_REQUEST)
                .forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }
}