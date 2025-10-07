package com.example.blog.repository;

import com.example.blog.entity.Story;
import com.example.blog.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StoryRepository extends JpaRepository<Story, Long> {

    // Get all stories for a specific user that haven't expired, oldest first, and not heavily flagged
    @Query("""
        SELECT s FROM Story s
        WHERE s.author = :author
          AND s.expiresAt > :now
          AND s.flagCount < 3
        ORDER BY s.createdAt ASC
    """)
    List<Story> findByAuthorAndExpiresAtAfterOrderByCreatedAtAsc(@Param("author") AppUser author,
                                                                 @Param("now") LocalDateTime now);

    // Get all stories that haven't expired, oldest first, and not heavily flagged
    @Query("""
        SELECT s FROM Story s
        WHERE s.expiresAt > :now
          AND s.flagCount < 3
        ORDER BY s.createdAt ASC
    """)
    List<Story> findByExpiresAtAfterOrderByCreatedAtAsc(@Param("now") LocalDateTime now);

    // Optional: get all stories excluding ones the given user has viewed, oldest first, and not heavily flagged
    @Query("""
        SELECT s FROM Story s
        LEFT JOIN s.viewers v
        WHERE s.expiresAt > :now
          AND (v IS NULL OR :user NOT MEMBER OF s.viewers)
          AND s.flagCount < 3
        ORDER BY s.createdAt ASC
    """)
    List<Story> findUnviewedStoriesForUser(@Param("now") LocalDateTime now, @Param("user") AppUser user);
}
