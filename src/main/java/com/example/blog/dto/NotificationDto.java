package com.example.blog.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationDto {
    private Long id;
    private String type; // FOLLOW, FOLLOW_REQUEST, FOLLOW_ACCEPTED, etc.
    private boolean read;
    private String senderUsername;
    private String recipientUsername;
    private Long followId; // only set if this notification is related to a follow
    private LocalDateTime createdAt;
    private String trunkName;
    private String songTitle;
    private String songArtist;
    private String albumArtUrl;
}
