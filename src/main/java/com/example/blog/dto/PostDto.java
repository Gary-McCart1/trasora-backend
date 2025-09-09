
package com.example.blog.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostDto {
    private Long id;
    private String authorUsername;
    private String authorProfilePictureUrl;
    private Long authorId;
    private String title;
    private String text;

    // Spotify fields
    private String trackId;
    private String trackName;
    private String artistName;
    private String albumArtUrl;

    // ✅ SoundCloud fields
    private String soundcloudTrackId;
    private String soundcloudTrackName;
    private String soundcloudArtistName;
    private String soundcloudAlbumArtUrl;
    private String soundcloudStreamUrl;

    private String customImageUrl;
    private String customVideoUrl;
    private Float trackVolume;
    private String s3Key;

    private String createdAt;
    private int likesCount;
    private Boolean likedByCurrentUser;
    private List<CommentDto> comments;
    private int branchCount;
    private boolean isPublic;
}


