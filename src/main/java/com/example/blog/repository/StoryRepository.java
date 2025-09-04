package com.example.blog.repository;

import com.example.blog.entity.Story;
import com.example.blog.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StoryRepository extends JpaRepository<Story, Long> {

    // Get all stories for a specific user that haven't expired, oldest first
    List<Story> findByAuthorAndExpiresAtAfterOrderByCreatedAtAsc(AppUser author, LocalDateTime now);

    // Get all stories that haven't expired, for feeds or StoriesBar, oldest first
    List<Story> findByExpiresAtAfterOrderByCreatedAtAsc(LocalDateTime now);

    // Optional: get all stories excluding ones the given user has viewed, oldest first
    @Query("SELECT s FROM Story s LEFT JOIN s.viewers v " +
            "WHERE s.expiresAt > :now AND (v IS NULL OR :user NOT MEMBER OF s.viewers) " +
            "ORDER BY s.createdAt ASC")
    List<Story> findUnviewedStoriesForUser(LocalDateTime now, AppUser user);
}
