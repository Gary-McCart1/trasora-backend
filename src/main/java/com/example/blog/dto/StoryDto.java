package com.example.blog.dto;

import com.example.blog.entity.Story.StoryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoryDto {

    private Long id;

    private Long authorId;
    private String authorUsername;
    private String authorProfilePictureUrl;
    private String trackId;
    private String trackName;
    private String artistName;
    private String albumArtUrl;

    private String contentUrl;  // S3 URL or track ID
    private String caption;

    private StoryType type;     // IMAGE, VIDEO, TRACK

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    private boolean viewed;     // has the current user viewed this story
}
