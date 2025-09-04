package com.example.blog.controller;

import com.example.blog.dto.StoryDto;
import com.example.blog.entity.AppUser;
import com.example.blog.repository.UserRepository;
import com.example.blog.service.StoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/stories")
@RequiredArgsConstructor
public class StoryController {

    private final StoryService storyService;
    private final UserRepository userRepository;

    private AppUser getAppUserFromPrincipal(UserDetails principal) {
        if (principal == null) {
            return null;
        }
        return userRepository.findByUsername(principal.getUsername()).orElse(null);
    }

    /**
     * Create a new story (with image upload)
     */
    @PostMapping("/create")
    public ResponseEntity<StoryDto> createStory(
            @AuthenticationPrincipal UserDetails principal,
            @RequestPart(value = "file", required = false) MultipartFile file, // optional file
            @RequestPart("story") StoryDto storyDto  // JSON part
    ) {
        AppUser currentUser = getAppUserFromPrincipal(principal);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            StoryDto createdStory = storyService.createStory(currentUser, file, storyDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdStory);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    /**
     * Get all active (non-expired) stories
     */
    @GetMapping("/active")
    public ResponseEntity<List<StoryDto>> getActiveStories(@AuthenticationPrincipal UserDetails principal) {
        AppUser currentUser = getAppUserFromPrincipal(principal);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<StoryDto> stories = storyService.getActiveStories(currentUser);
        return ResponseEntity.ok(stories);
    }

    /**
     * Get all stories for a specific user
     */
    @GetMapping("/user/{username}")
    public ResponseEntity<List<StoryDto>> getUserStories(@PathVariable String username) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        List<StoryDto> stories = storyService.getUserStories(user);
        return ResponseEntity.ok(stories);
    }

    /**
     * Delete a story (only by owner)
     */
    @DeleteMapping("/{storyId}")
    public ResponseEntity<Void> deleteStory(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long storyId
    ) {
        AppUser currentUser = getAppUserFromPrincipal(principal);

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            storyService.deleteStory(storyId, currentUser);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (("Story not found: " + storyId).equals(msg)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            } else if ("You do not have permission to delete this story".equals(msg)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }
    }


    /**
     * Mark a story as viewed by the current user
     */
    @PostMapping("/{storyId}/view")
    public ResponseEntity<Void> markStoryAsViewed(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long storyId
    ) {
        AppUser currentUser = getAppUserFromPrincipal(principal);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            storyService.markStoryAsViewed(storyId, currentUser);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
