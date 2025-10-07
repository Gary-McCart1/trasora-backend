package com.example.blog.controller;

import com.example.blog.entity.AppUser;
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

    public FlagController(FlagService flagService, UserRepository userRepository) {
        this.flagService = flagService;
        this.userRepository = userRepository;
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
}
