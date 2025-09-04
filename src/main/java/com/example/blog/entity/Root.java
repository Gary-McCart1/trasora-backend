package com.example.blog.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "roots")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Root {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Which user this root belongs to
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"branches", "hibernateLazyInitializer"})
    private AppUser user;

    // Store Spotify track ID for reference
    @Column(nullable = false)
    private String trackId;

    // Optional: store cached info for display
    private String trackTitle;
    private String artistName;
    private String albumArtUrl;

    // Order/position in top 10
    @Column(nullable = false)
    private int position;
}


