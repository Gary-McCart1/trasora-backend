package com.example.blog.controller;

import com.example.blog.dto.NotificationDto;
import com.example.blog.entity.AppUser;
import com.example.blog.service.NotificationService;
import com.example.blog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping("/unread")
    public List<NotificationDto> getUnreadNotifications(@AuthenticationPrincipal UserDetails userDetails) {
        AppUser user = getCurrentUser(userDetails);
        return notificationService.toDtoList(notificationService.getUnreadNotifications(user));
    }

    @GetMapping
    public List<NotificationDto> getAllNotifications(@AuthenticationPrincipal UserDetails userDetails) {
        AppUser user = getCurrentUser(userDetails);
        return notificationService.toDtoList(notificationService.getAllNotifications(user));
    }

    @PostMapping("/{id}/read")
    public void markNotificationAsRead(@PathVariable Long id,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        AppUser user = getCurrentUser(userDetails);
        notificationService.markAsRead(id, user);
    }

    @PostMapping("/read-all")
    public void markAllAsRead(@AuthenticationPrincipal UserDetails userDetails) {
        AppUser user = getCurrentUser(userDetails);
        notificationService.markAllAsRead(user);
    }

    private AppUser getCurrentUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping("/read-all-except-follow")
    public void markAllExceptFollowRequestsAsRead(@AuthenticationPrincipal UserDetails userDetails) {
        AppUser user = getCurrentUser(userDetails);
        notificationService.markAllExceptFollowRequestsAsRead(user);
    }

}
