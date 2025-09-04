package com.example.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReferralDto {
    private String username;
    private int referralCount;
    private String profilePictureUrl;

    public void incrementCount() {
        this.referralCount++;
    }
}

