package com.example.blog.dto;

import com.example.blog.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String fullName;
    private String email;
    private String username;
    private String bio;
    private Date joinedAt;
    private String profilePictureUrl;
    private Role role;
    private int followersCount;
    private int followingCount;
    private String accentColor;
    private boolean spotifyConnected;
    private boolean profilePublic;
    private int branchCount;
    private boolean spotifyPremium;
    private String referredBy;
    private String pushSubscriptionEndpoint;
    private String pushSubscriptionKeysP256dh;
    private String pushSubscriptionKeysAuth;

}
