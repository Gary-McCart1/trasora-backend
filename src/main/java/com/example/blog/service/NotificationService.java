package com.example.blog.service;

import com.example.blog.dto.NotificationDto;
import com.example.blog.entity.AppUser;
import com.example.blog.entity.Follow;
import com.example.blog.entity.Notification;
import com.example.blog.entity.NotificationType;
import com.example.blog.entity.Post;
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
    private final PushNotificationService pushNotificationService;

    /**
     * Generic notification creator for any type.
     * Handles Post, Follow, and Branch metadata.
     */
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

        // Post-based notifications
        if (type == NotificationType.LIKE || type == NotificationType.COMMENT) {
            notification.setPost(post);
        }

        // Follow-based notifications
        if ((type == NotificationType.FOLLOW || type == NotificationType.FOLLOW_REQUEST || type == NotificationType.FOLLOW_ACCEPTED) && follow != null) {
            notification.setFollow(follow);
        }

        // Branch-based notifications
        if (type == NotificationType.BRANCH_ADDED) {
            notification.setTrunkName(trunkName);
            notification.setSongTitle(songTitle);
            notification.setSongArtist(songArtist);
            notification.setAlbumArtUrl(albumArtUrl);
        }

        // Save to DB
        Notification saved = notificationRepository.save(notification);
        System.out.println("✅ Notification saved with ID: " + saved.getId());

        // Send push
        if (recipient.getPushSubscriptionEndpoint() != null) {
            String title = "New " + type.name();
            String body = sender.getUsername() + " ";
            String imageUrl = null;
            String url = "/"; // default

            switch (type) {
                case LIKE -> {
                    body += "liked your post";
                    if (post != null) {
                        imageUrl = post.getCustomImageUrl() != null ? post.getCustomImageUrl() : post.getAlbumArtUrl();
                        url = "/post/" + post.getId();
                    }
                }
                case COMMENT -> {
                    body += "commented on your post";
                    if (post != null) {
                        imageUrl = post.getCustomImageUrl() != null ? post.getCustomImageUrl() : post.getAlbumArtUrl();
                        url = "/post/" + post.getId();
                    }
                }
                case FOLLOW -> {
                    body += "followed you";
                    url = "/profile/" + sender.getId();
                }
                case FOLLOW_REQUEST -> {
                    body += "sent you a follow request";
                    url = "/profile/" + sender.getId();
                }
                case FOLLOW_ACCEPTED -> {
                    body += "accepted your follow request";
                    url = "/profile/" + sender.getId();
                }
                case BRANCH_ADDED -> {
                    body += "added a song to " + trunkName;
                    imageUrl = albumArtUrl;
                    url = "/trunk/" + trunkName;
                }
            }

            System.out.println("🚀 Sending push notification: recipient=" + recipient.getUsername() + ", title=" + title + ", body=" + body + ", url=" + url);
            pushNotificationService.sendPushNotification(recipient, title, body, imageUrl, url);
        } else {
            System.out.println("⚠️ Recipient " + recipient.getUsername() + " has no push subscription. Skipping push notification.");
        }

        return saved;
    }



    // =========================
    // Convenience creators
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

    public Notification createBranchNotification(
            AppUser recipient,
            AppUser sender,
            String trunkName,
            String songTitle,
            String songArtist,
            String albumArtUrl
    ) {
        return createNotification(recipient, sender, NotificationType.BRANCH_ADDED, null, null,
                trunkName, songTitle, songArtist, albumArtUrl);
    }

    // =========================
    // Retrieval and update logic
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
        if (!notification.getRecipient().equals(recipient)) {
            throw new RuntimeException("Not authorized to mark this notification");
        }
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
        if (!notification.getRecipient().equals(recipient)) {
            throw new RuntimeException("Not authorized to mark this notification");
        }
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
        if (!notifications.isEmpty()) {
            notificationRepository.deleteAll(notifications);
        }
    }

    /**
     * Converts Notification entities to DTOs to avoid Hibernate proxy serialization issues.
     * Includes branch metadata fields.
     */
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

            // Add branch info if present
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
