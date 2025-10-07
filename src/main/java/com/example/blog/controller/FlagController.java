package com.example.blog.controller;

import com.example.blog.entity.AppUser;
import com.example.blog.entity.Flag;
import com.example.blog.repository.UserRepository;
import com.example.blog.service.FlagService;
import com.example.blog.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<?> flagPost(
            @PathVariable Long postId,
            @RequestParam Long reporterId,
            @RequestParam String reason) {

        AppUser reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        flagService.flagPost(postId, reporter, reason);
        return ResponseEntity.ok("Post flagged successfully");
    }

    @PostMapping("/comment/{commentId}")
    public ResponseEntity<?> flagComment(
            @PathVariable Long commentId,
            @RequestParam Long reporterId,
            @RequestParam String reason) {

        AppUser reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        flagService.flagComment(commentId, reporter, reason);
        return ResponseEntity.ok("Comment flagged successfully");
    }

    @PostMapping("/story/{storyId}")
    public ResponseEntity<?> flagStory(
            @PathVariable Long storyId,
            @RequestParam Long reporterId,
            @RequestParam String reason) {

        AppUser reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        flagService.flagStory(storyId, reporter, reason);
        return ResponseEntity.ok("Story flagged successfully");
    }
}
