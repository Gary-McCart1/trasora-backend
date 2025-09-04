package com.example.blog.entity;

import com.example.blog.entity.AppUser;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "branches",
        uniqueConstraints = @UniqueConstraint(columnNames = {"trunk_id", "spotifyTrackId"}))
public class Branch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String spotifyTrackId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String artist;

    @Column(nullable = false)
    private String albumArtUrl;

    @Column(nullable = false, columnDefinition = "int default 0")
    private int position = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by_id")
    @ToString.Exclude
    private AppUser addedBy;

    private LocalDateTime addedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trunk_id")
    @JsonBackReference
    @ToString.Exclude
    private Trunk trunk;

    @PrePersist
    protected void onCreate() {
        addedAt = LocalDateTime.now();
    }
}

