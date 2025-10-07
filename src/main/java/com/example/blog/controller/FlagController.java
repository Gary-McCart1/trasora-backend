package com.example.blog.controller;

import com.example.blog.entity.AppUser;
import com.example.blog.entity.Comment;
import com.example.blog.entity.Post;
import com.example.blog.entity.Story;
import com.example.blog.repository.CommentRepository;
import com.example.blog.repository.PostRepository;
import com.example.blog.repository.StoryRepository;
import com.example.blog.service.FlagService;
import com.example.blog.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/flags")
public class FlagController {

    private final FlagService flagService;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final StoryRepository storyRepository;

    public FlagController(FlagService flagService, UserRepository userRepository, PostRepository postRepository, CommentRepository commentRepository, StoryRepository storyRepository) {
        this.flagService = flagService;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.storyRepository = storyRepository;
    }

    @PostMapping("/post/{postId}")
    public ResponseEntity<Map<String, Object>> flagPost(
            @PathVariable Long postId,
            @RequestParam Long reporterId,
            @RequestParam String reason
    ) {
        AppUser reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        flagService.flagPost(postId, reporter, reason);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Post flagged successfully.");
        response.put("postId", postId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/comment/{commentId}")
    public ResponseEntity<Map<String, Object>> flagComment(
            @PathVariable Long commentId,
            @RequestParam Long reporterId,
            @RequestParam String reason
    ) {
        AppUser reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        flagService.flagComment(commentId, reporter, reason);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Comment flagged successfully.");
        response.put("commentId", commentId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/story/{storyId}")
    public ResponseEntity<Map<String, Object>> flagStory(
            @PathVariable Long storyId,
            @RequestParam Long reporterId,
            @RequestParam String reason
    ) {
        AppUser reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        flagService.flagStory(storyId, reporter, reason);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Story flagged successfully.");
        response.put("storyId", storyId);

        return ResponseEntity.ok(response);
    }

    // FlagController.java
    @PostMapping("/review/{contentType}/{contentId}")
    public ResponseEntity<Map<String, Object>> reviewContent(
            @PathVariable String contentType,
            @PathVariable Long contentId,
            @RequestParam boolean hide) {

        switch (contentType.toLowerCase()) {
            case "post":
                Post post = postRepository.findById(contentId)
                        .orElseThrow(() -> new RuntimeException("Post not found"));
                post.setHidden(hide);
                if (!hide) post.setFlagCount(0);
                postRepository.save(post);
                break;
            case "comment":
                Comment comment = commentRepository.findById(contentId)
                        .orElseThrow(() -> new RuntimeException("Comment not found"));
                comment.setHidden(hide);
                if (!hide) comment.setFlagCount(0);
                commentRepository.save(comment);
                break;
            case "story":
                Story story = storyRepository.findById(contentId)
                        .orElseThrow(() -> new RuntimeException("Story not found"));
                story.setHidden(hide);
                if (!hide) story.setFlagCount(0);
                storyRepository.save(story);
                break;
            default:
                throw new RuntimeException("Invalid content type");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", hide ? "Content hidden" : "Content unhidden");
        response.put("contentType", contentType);
        response.put("contentId", contentId);

        return ResponseEntity.ok(response);
    }

}
