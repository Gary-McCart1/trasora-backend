package com.example.blog.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Flag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Who reported the content
    @ManyToOne(optional = false)
    private AppUser reporter;

    // The user who owns the flagged content
    @ManyToOne(optional = false)
    private AppUser reportedUser;

    // Optional links to what was flagged
    @ManyToOne
    private Post post;

    @ManyToOne
    private Story story;

    @ManyToOne
    private Comment comment;

    @Column(nullable = false)
    private String reason; // e.g. "Hate speech", "Spam", etc.

    @Builder.Default
    private boolean reviewed = false; // Marked true when admin reviews it

    private LocalDateTime createdAt;

    private boolean resolved = false;
}
