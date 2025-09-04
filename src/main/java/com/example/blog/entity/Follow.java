package com.example.blog.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.Instant;

@Entity
@Table(name = "follows", uniqueConstraints = @UniqueConstraint(columnNames = {"follower_id", "following_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Follow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "follower_id", nullable = false)
    private AppUser follower;

    @ManyToOne
    @JoinColumn(name = "following_id", nullable = false)
    private AppUser following;

    @CreatedDate
    private Instant followedAt;

    private boolean accepted = false;
}

