package com.example.blog.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Story {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    @JsonIgnore
    private AppUser author;
    private String trackId;         // Spotify track ID (required)
    private String trackName;
    private String artistName;
    private String albumArtUrl;

    private String applePreviewUrl;

    // Either image/video URL or Spotify track ID
    private String contentUrl;    // S3 URL or track ID
    private String s3Key;         // S3 key for deletion (if media)

    @Enumerated(EnumType.STRING)
    private StoryType type;       // IMAGE, VIDEO, TRACK

    private String caption;       // optional caption

    @CreationTimestamp
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt; // auto-set to 24h from createdAt

    @ManyToMany
    @JoinTable(
            name = "story_views",
            joinColumns = @JoinColumn(name = "story_id"),
            inverseJoinColumns = @JoinColumn(name = "viewer_id")
    )
    @JsonIgnore
    private Set<AppUser> viewers = new HashSet<>();

    public boolean isViewedByUser(AppUser user) {
        return viewers.contains(user);
    }

    @PrePersist
    public void setDefaultExpiry() {
        if (expiresAt == null) {
            expiresAt = createdAt.plusHours(24);
        }
    }

    public enum StoryType {
        IMAGE,
        VIDEO,
        TRACK
    }
}
