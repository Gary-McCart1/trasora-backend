package com.example.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SuggestedUserDto {
    private Long id;
    private String username;
    private String fullName;
    private String profilePictureUrl;
    private String accentColor;
}
