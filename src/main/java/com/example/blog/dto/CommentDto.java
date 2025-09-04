package com.example.blog.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommentDto {
    private Long id;
    private String authorUsername;
    private String authorProfilePictureUrl;
    private String commentText;
    private Long postId;
    private LocalDateTime createdAt;
}
