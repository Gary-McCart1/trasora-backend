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
}
