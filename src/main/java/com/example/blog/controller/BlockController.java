package com.example.blog.controller;

import com.example.blog.entity.AppUser;
import com.example.blog.service.BlockService;
import com.example.blog.service.UserService;
import com.example.blog.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/block")
@RequiredArgsConstructor
public class BlockController {

    private final BlockService blockService;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    /** Helper to get current user from JWT */
    private AppUser getCurrentUser(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        String username = jwtUtil.extractUsername(token);
        return userService.findByUsernameOrEmail(username);
    }

    /** Block a user */
    @PostMapping("/{username}")
    public ResponseEntity<?> blockUser(@PathVariable String username, HttpServletRequest request) {
        AppUser currentUser = getCurrentUser(request);
        AppUser targetUser = userService.getUserByUsername(username);
        if (targetUser == null) return ResponseEntity.notFound().build();

        blockService.blockUser(currentUser, targetUser.getId());
        return ResponseEntity.ok().body("User blocked successfully");
    }

    /** Unblock a user */
    @DeleteMapping("/{username}")
    public ResponseEntity<?> unblockUser(@PathVariable String username, HttpServletRequest request) {
        AppUser currentUser = getCurrentUser(request);
        AppUser targetUser = userService.getUserByUsername(username);
        if (targetUser == null) return ResponseEntity.notFound().build();

        blockService.unblockUser(currentUser, targetUser.getId());
        return ResponseEntity.ok().body("User unblocked successfully");
    }

    /** Check if current user has blocked a specific user */
    @GetMapping("/{username}/status")
    public ResponseEntity<?> getBlockStatus(@PathVariable String username, HttpServletRequest request) {
        AppUser currentUser = getCurrentUser(request);
        AppUser targetUser = userService.getUserByUsername(username);
        if (targetUser == null) return ResponseEntity.notFound().build();

        boolean isBlocked = blockService.isBlocked(currentUser, targetUser);
        return ResponseEntity.ok().body(Map.of("blocked", isBlocked));
    }
}
