package com.example.blog.controller;

import com.example.blog.entity.Flag;
import com.example.blog.service.FlagService;
import com.example.blog.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/flags")
public class FlagController {

    private final FlagService flagService;
    private final UserService userService;

    public FlagController(FlagService flagService, UserService userService) {
        this.flagService = flagService;
        this.userService = userService;
    }

    @PostMapping("/post/{postId}")
    public Flag flagPost(@PathVariable Long postId, @RequestParam String reason) {
        Long reporterId = userService.getCurrentUser().getId();
        return flagService.flagPost(reporterId, postId, reason);
    }

    @PostMapping("/comment/{commentId}")
    public Flag flagComment(@PathVariable Long commentId, @RequestParam String reason) {
        Long reporterId = userService.getCurrentUser().getId();
        return flagService.flagComment(reporterId, commentId, reason);
    }

    @PostMapping("/story/{storyId}")
    public Flag flagStory(@PathVariable Long storyId, @RequestParam String reason) {
        Long reporterId = userService.getCurrentUser().getId();
        return flagService.flagStory(reporterId, storyId, reason);
    }

    @GetMapping("/unreviewed")
    public List<Flag> getUnreviewedFlags() {
        return flagService.getUnreviewedFlags();
    }

    @PostMapping("/{flagId}/reviewed")
    public void markFlagReviewed(@PathVariable Long flagId) {
        flagService.markFlagReviewed(flagId);
    }
}
