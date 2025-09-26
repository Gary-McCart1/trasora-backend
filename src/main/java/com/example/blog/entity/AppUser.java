package com.example.blog.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Data
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fullName;
    @JsonIgnore
    private String email;
    private String username;
    @JsonIgnore
    private String password;

    // Account info
    @Column(length = 1000)
    private String bio;
    private Date joinedAt;
    private String profilePictureUrl;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;
    private String accentColor;

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Post> posts = new ArrayList<>();

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Trunk> trunks = new ArrayList<>();

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean spotifyConnected = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean verified = false;
    private String verificationToken;

    @Column(name = "password_reset_token")
    private String passwordResetToken;

    @Column(name = "password_reset_token_expiry")
    private LocalDateTime passwordResetTokenExpiry;

    @Column(nullable = false)
    private boolean isProfilePublic = false;

    @Column(length = 2000)
    private String spotifyAccessToken;

    @Column(length = 2000)
    private String spotifyRefreshToken;

    @OneToMany(mappedBy = "follower", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Follow> following = new ArrayList<>();

    @OneToMany(mappedBy = "following", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Follow> followers = new ArrayList<>();

    @OneToMany(mappedBy = "recipient", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Notification> receivedNotifications = new ArrayList<>();

    @OneToMany(mappedBy = "sender", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Notification> sentNotifications = new ArrayList<>();

    @Column(nullable = false)
    private boolean spotifyPremium;

    private String referredBy;

    @Column(length = 2048)
    private String pushSubscriptionEndpoint;

    @Column(length = 512)
    private String pushSubscriptionKeysP256dh;

    @Column(length = 512)
    private String pushSubscriptionKeysAuth;


    @Column
    private int branchCount;
}
