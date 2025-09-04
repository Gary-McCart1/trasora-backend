package com.example.blog.dto;

import com.example.blog.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String username;
    private String email;
    private String fullName;
    private String bio;
    private Role role;
    private Date joinedAt;
    private int followersCount;
    private int followingCount;
    private boolean isFollowing; // optional for future use
    private boolean profilePublic;
}
