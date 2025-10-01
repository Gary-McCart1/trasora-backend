package com.example.blog.controller;

import com.example.blog.dto.SuggestedUserDto;
import com.example.blog.entity.AppUser;
import com.example.blog.service.FollowService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/follow")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    /**
     * Follow a user (handles public and private accounts).
     * Returns updated status and follower/following counts.
     */
    @PostMapping("/{username}")
    public ResponseEntity<?> follow(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Returns "following" or "requested" depending on public/private account
        String status = followService.followUser(userDetails.getUsername(), username);

        Long userId = followService.getUserIdByUsername(username);
        long followersCount = followService.getFollowerCount(userId);
        long followingCount = followService.getFollowingCount(userId);

        return ResponseEntity.ok(Map.of(
                "status", status,                  // "following" or "requested"
                "followersCount", followersCount,
                "followingCount", followingCount
        ));
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<?> unfollow(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails) {

        followService.unfollowUser(userDetails.getUsername(), username);

        Long userId = followService.getUserIdByUsername(username);
        long followersCount = followService.getFollowerCount(userId);
        long followingCount = followService.getFollowingCount(userId);

        return ResponseEntity.ok(Map.of(
                "status", "not-following",
                "followersCount", followersCount,
                "followingCount", followingCount
        ));
    }


    /**
     * Get follower count for a user.
     */
    @GetMapping("/{id}/followers/count")
    public ResponseEntity<Long> getFollowerCount(@PathVariable Long id) {
        return ResponseEntity.ok(followService.getFollowerCount(id));
    }

    /**
     * Get following count for a user.
     */
    @GetMapping("/{id}/following/count")
    public ResponseEntity<Long> getFollowingCount(@PathVariable Long id) {
        return ResponseEntity.ok(followService.getFollowingCount(id));
    }

    /**
     * Check if the authenticated user is following the given user.
     */
    @GetMapping("/{id}/is-following")
    public ResponseEntity<Boolean> isFollowing(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(followService.isFollowing(userDetails.getUsername(), id));
    }

    /**
     * Get follow status for authenticated user with respect to a target user.
     * Returns status + follower/following counts.
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<?> getFollowStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        String status = followService.getFollowStatus(userDetails.getUsername(), id);
        long followersCount = followService.getFollowerCount(id);
        long followingCount = followService.getFollowingCount(id);

        return ResponseEntity.ok(Map.of(
                "status", status,
                "followersCount", followersCount,
                "followingCount", followingCount
        ));
    }

    /**
     * Accept a pending follow request.
     */
    @PostMapping("/{id}/accept")
    public ResponseEntity<?> acceptFollowRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            followService.acceptFollowRequest(id, userDetails.getUsername());
            return ResponseEntity.ok(Map.of("message", "Follow request accepted"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Reject a pending follow request.
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectFollowRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        followService.rejectFollowRequest(id, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Follow request rejected"));
    }

    @GetMapping("/suggested/{username}")
    public List<SuggestedUserDto> getSuggestedFollows(@PathVariable String username) {
        List<AppUser> users = followService.getSuggestedFollows(username);

        // Map to lightweight DTO
        return users.stream()
                .map(u -> new SuggestedUserDto(
                        u.getId(),
                        u.getUsername(),
                        u.getFullName(),
                        u.getProfilePictureUrl(),
                        u.getAccentColor()
                ))
                .collect(Collectors.toList());
    }




}
