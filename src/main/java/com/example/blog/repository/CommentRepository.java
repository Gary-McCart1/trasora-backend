package com.example.blog.repository;

import com.example.blog.entity.AppUser;
import com.example.blog.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Query("""
        SELECT c FROM Comment c
        WHERE c.post.id = :postId
          AND NOT EXISTS (
              SELECT 1
              FROM Block b
              WHERE b.blocker = :currentUser AND b.blocked = c.author
          )
        ORDER BY c.createdAt ASC
    """)
    List<Comment> getVisibleCommentsByPost(@Param("postId") Long postId,
                                           @Param("currentUser") AppUser currentUser);
}
