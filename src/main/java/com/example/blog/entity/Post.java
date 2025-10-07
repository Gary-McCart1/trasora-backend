package com.example.blog.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    @JsonIgnore
    private AppUser author;

    private String title;
    private String text;

    // Spotify fields
    private String trackId;
    private String trackName;
    private String artistName;
    private String albumArtUrl;

    // Apple Music fields
    private String appleTrackId;
    private String appleTrackName;
    private String appleArtistName;
    private String appleAlbumArtUrl;
    private String applePreviewUrl; // 30-second preview


    private String customVideoUrl;
    private Float trackVolume;

    private String customImageUrl;
    private String s3Key;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Comment> comments = new ArrayList<>();

    @Column(nullable = false)
    private int branchCount;

    @ManyToMany
    @JoinTable(
            name = "post_likes",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @JsonIgnore
    private Set<AppUser> likedBy = new HashSet<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Notification> notifications = new ArrayList<>();

    private int flagCount = 0;
    private boolean hidden = false;

    public boolean isLikedByUser(AppUser user) {
        return likedBy.contains(user);
    }
}

