package com.example.blog.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "posts", "notifications"})
    private AppUser recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "posts", "notifications"})
    private AppUser sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "author", "comments"})
    private Post post;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private boolean isRead = false;

    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follow_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Follow follow; // only set for FOLLOW_REQUEST notifications

    private String trunkName;
    private String songTitle;
    private String songArtist;
    private String albumArtUrl;

    /**
     * JSON-friendly getter for follow ID
     */
    public Long getFollowId() {
        return follow != null ? follow.getId() : null;
    }
}

