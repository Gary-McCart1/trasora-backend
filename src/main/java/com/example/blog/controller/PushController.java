package com.example.blog.controller;

import com.example.blog.entity.AppUser;
import com.example.blog.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/push")
public class PushController {

    private final UserService userService;
    private final AuthController authController; // for authenticateRequest

    public PushController(UserService userService, AuthController authController) {
        this.userService = userService;
        this.authController = authController;
    }

    @Data
    public static class PushSubscriptionRequest {
        private String endpoint;
        private String expirationTime; // can be null
        private String keysP256dh;
        private String keysAuth;
    }

    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe(@RequestBody PushSubscriptionRequest subscriptionRequest,
                                       HttpServletRequest request) {
        AppUser currentUser = authController.authenticateRequest(request);

        userService.savePushSubscription(currentUser.getUsername(), subscriptionRequest);

        return ResponseEntity.ok("Push subscription saved successfully");
    }

    @DeleteMapping("/unsubscribe")
    public ResponseEntity<?> unsubscribe(HttpServletRequest request) {
        AppUser currentUser = authController.authenticateRequest(request);

        boolean success = userService.removePushSubscription(currentUser.getUsername());

        if (success) {
            return ResponseEntity.ok("Push subscription removed successfully");
        } else {
            return ResponseEntity.status(404).body("No push subscription found to remove");
        }
    }

    @GetMapping("/subscription/{username}")
    public ResponseEntity<?> getSubscription(@PathVariable String username,
                                             HttpServletRequest request) {
        AppUser currentUser = authController.authenticateRequest(request);

        // Only allow fetching own subscription
        if (!currentUser.getUsername().equals(username)) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        PushSubscriptionRequest subscription = userService.getPushSubscription(username);

        if (subscription == null || subscription.getEndpoint() == null) {
            return ResponseEntity.status(404).body("No push subscription found");
        }

        return ResponseEntity.ok(subscription);
    }
}
