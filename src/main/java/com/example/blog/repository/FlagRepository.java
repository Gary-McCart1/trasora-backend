package com.example.blog.repository;

import com.example.blog.entity.Flag;
import com.example.blog.entity.AppUser;
import com.example.blog.entity.Post;
import com.example.blog.entity.Comment;
import com.example.blog.entity.Story;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FlagRepository extends JpaRepository<Flag, Long> {

    List<Flag> findByReviewedFalse();
    List<Flag> findByReporter(AppUser reporter);

    // Duplicate-check helpers:
    boolean existsByReporterAndPost(AppUser reporter, Post post);
    boolean existsByReporterAndComment(AppUser reporter, Comment comment);
    boolean existsByReporterAndStory(AppUser reporter, Story story);
}
