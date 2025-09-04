package com.example.blog.repository;

import com.example.blog.entity.AppUser;
import com.example.blog.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByAuthorOrderByCreatedAtDesc(AppUser author);
    List<Post> findAllByOrderByCreatedAtDesc();

    @Query("""
       SELECT p FROM Post p
       WHERE p.author.isProfilePublic = true
          OR p.author = :currentUser
          OR p.author.id IN (
               SELECT f.following.id
               FROM Follow f
               WHERE f.follower = :currentUser AND f.accepted = true
          )
       ORDER BY p.createdAt DESC
       """)
    List<Post> findFeedPosts(@Param("currentUser") AppUser currentUser);

    @Query("""
        SELECT p FROM Post p WHERE p.author = :currentUser
        AND p.customVideoUrl = NULL
    """)
    List<Post> findImagePosts(@Param("currentUser") AppUser currentUser);

    @Query("""
        SELECT p FROM Post p WHERE p.author = :currentUser
        AND p.customImageUrl = NULL
    """)
    List<Post> findVideoPosts(@Param("currentUser") AppUser currentUser);
}
