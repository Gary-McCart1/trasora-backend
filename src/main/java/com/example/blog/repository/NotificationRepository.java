package com.example.blog.repository;

import com.example.blog.entity.AppUser;
import com.example.blog.entity.Follow;
import com.example.blog.entity.Notification;
import com.example.blog.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Unread notifications, newest first
    List<Notification> findByRecipientAndIsReadFalseOrderByCreatedAtDesc(AppUser recipient);

    // All notifications for a recipient, newest first
    List<Notification> findByRecipientOrderByCreatedAtDesc(AppUser recipient);

    List<Notification> findBySenderAndRecipientAndTypeAndIsReadFalse(
            AppUser sender,
            AppUser recipient,
            NotificationType type
    );
    List<Notification> findByFollow(Follow follow);
    List<Notification> findByFollowAndType(Follow follow, NotificationType type);
    List<Notification> findAllByFollow_Id(Long followId);

}
