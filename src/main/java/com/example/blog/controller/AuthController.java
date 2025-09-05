package com.example.blog.controller;

import com.example.blog.dto.*;
import com.example.blog.entity.AppUser;
import com.example.blog.repository.UserRepository;
import com.example.blog.service.S3Service;
import com.example.blog.service.UserService;
import com.example.blog.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final S3Service s3Service;
    private final UserRepository userRepository;

    @Data
    public static class EditUserRequest {
        private String bio;
        private String profilePicture;
    }

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil,
                          UserService userService, PasswordEncoder passwordEncoder, S3Service s3Service, UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.s3Service = s3Service;
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        String token = jwtUtil.extractTokenFromCookie(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
        }

        String username = jwtUtil.extractUsername(token);
        AppUser user = userService.findByUsernameOrEmail(username);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        UserDto userDto = mapToUserDto(user);

        return ResponseEntity.ok(userDto);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getLogin(), request.getPassword())
            );
            org.springframework.security.core.userdetails.User userDetails =
                    (org.springframework.security.core.userdetails.User) authentication.getPrincipal();

            AppUser user = userService.findByUsernameOrEmail(userDetails.getUsername());
            if (!user.isVerified()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Please verify your email before logging in.");
            }


            String token = jwtUtil.generateToken(user.getUsername());

            ResponseCookie cookie = ResponseCookie.from("jwt", token)
                    .httpOnly(true)
                    .secure(true) // set true in production
                    .path("/")
                    .sameSite("None")
                    .maxAge(Duration.ofDays(7))
                    .build();

            response.setHeader("Set-Cookie", cookie.toString());

            // Assume false for isFollowing here (you can customize if needed)
            AuthResponse authResponse = mapToAuthResponse(user, token, false);

            return ResponseEntity.ok(authResponse);

        } catch (Exception e) {
            System.out.println("Login failed: " + e.getMessage());
            Map<String, String> error = Map.of("message", "Invalid username or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody RegisterRequest authRequest) {
        AppUser newUser = userService.registerUser(authRequest.getEmail(), authRequest.getUsername(),
                authRequest.getPassword(), authRequest.getFullName());

        // Generate verification token and save to user
        String token = userService.createVerificationToken(newUser);

        // Send verification email
        userService.sendVerificationEmail(newUser.getEmail(), token);

        return ResponseEntity.ok("User registered successfully. Please check your email to verify your account.");
    }


    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(true) // set true only in prod
                .path("/")
                .sameSite("None")
                .maxAge(0) // expire immediately
                .build();

        response.setHeader("Set-Cookie", cookie.toString());
        return ResponseEntity.ok("Logged out successfully");
    }


    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getUsers() {
        List<AppUser> users = userService.getUsers();
        List<UserDto> userDtos = users.stream()
                .map(this::mapToUserDto)
                .toList();
        return ResponseEntity.ok(userDtos);
    }

    @DeleteMapping("/user/{username}")
    public ResponseEntity<?> deleteUser(@PathVariable String username, Authentication authentication) {
        String loggedInUsername = authentication.getName();

        if (!loggedInUsername.equals(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can only delete your own account.");
        }

        userService.deleteUser(username);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<UserDto> getUserByUsername(@PathVariable String username) {
        AppUser user = userService.getUserByUsername(username);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        UserDto userDto = mapToUserDto(user);
        return ResponseEntity.ok(userDto);
    }

    @PutMapping(value = "/user", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> editUserProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart(value = "bio", required = false) String bio,
            @RequestPart(value = "profilePic", required = false) MultipartFile profilePic,
            @RequestPart(value = "accentColor", required = false) String accentColor) throws IOException {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }

        String profilePicUrl = null;
        if (profilePic != null && !profilePic.isEmpty()) {
            profilePicUrl = s3Service.uploadFile(
                    "profile-pictures/" + System.currentTimeMillis() + "-" + profilePic.getOriginalFilename(),
                    profilePic.getInputStream(),
                    profilePic.getSize(),
                    profilePic.getContentType());
        }

        AppUser updatedUser = userService.editUser(
                userDetails.getUsername(),
                bio,
                profilePicUrl,
                accentColor);

        UserDto updatedUserDto = mapToUserDto(updatedUser);

        return ResponseEntity.ok(updatedUserDto);
    }


    // Mapping helpers - ideally move to service or a dedicated mapper class
    private UserDto mapToUserDto(AppUser user) {
        return new UserDto(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getUsername(),
                user.getBio(),
                user.getJoinedAt(),
                user.getProfilePictureUrl(),
                user.getRole(),
                userService.getFollowersCount(user.getId()),
                userService.getFollowingCount(user.getId()),
                user.getAccentColor(),
                user.isSpotifyConnected(),
                user.isProfilePublic(),
                user.getBranchCount(),
                user.isSpotifyPremium(),
                user.getReferredBy()
        );
    }

    private AuthResponse mapToAuthResponse(AppUser user, String token, boolean isFollowing) {
        return new AuthResponse(
                token,
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getBio(),
                user.getRole(),
                user.getJoinedAt(),
                userService.getFollowersCount(user.getId()),
                userService.getFollowingCount(user.getId()),
                isFollowing,
                user.isProfilePublic()
        );
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchUsers(@RequestParam String q) {
        List<AppUser> users = userService.searchUsersByUsername(q, 5); // limit 5 results
        Map<String, Object> response = new HashMap<>();
        response.put("users", users);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search-bar")
    public ResponseEntity<Map<String, Object>> searchBarUsers(@RequestParam String q) {
        List<AppUser> users = userService.searchUsersByUsername(q, 5); // limit 5 results

        // Map each AppUser to UserDto
        List<UserDto> userDtos = users.stream()
                .map(this::mapToUserDto)
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("users", userDtos);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        Optional<AppUser> userOpt = userService.findByVerificationToken(token);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid verification token");
        }
        AppUser user = userOpt.get();
        user.setVerified(true);
        user.setVerificationToken(null);
        userRepository.save(user);
        return ResponseEntity.ok("Email verified successfully");
    }


    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        try {
            boolean sent = userService.sendPasswordResetEmail(email);
            if (!sent) {
                // Example: your service returns false if user not found
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "No account found for email: " + email));
            }
            return ResponseEntity.ok(Map.of("message", "Password reset email sent if the account exists."));
        } catch (Exception e) {
            // Log the exception on server side for debugging
            e.printStackTrace();
            // Return a user-friendly error message
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An error occurred while processing your request. Please try again later."));
        }
    }


    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest req) {
        userService.resetPassword(req.getToken(), req.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{username}/profile-visibility")
    public ResponseEntity<UserDto> updateProfileVisibility(
            @PathVariable String username,
            @RequestParam boolean profilePublic,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (!userDetails.getUsername().equals(username)) {
            return ResponseEntity.status(403).build(); // Forbidden
        }

        AppUser updatedUser = userService.updateProfileVisibility(username, profilePublic);
        return ResponseEntity.ok(mapToUserDto(updatedUser));
    }

    @PutMapping("/user/{username}/disconnect-spotify")
    public ResponseEntity<?> disconnectSpotify(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (userDetails == null || !userDetails.getUsername().equals(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "You can only disconnect your own Spotify account."));
        }

        userService.disconnectSpotify(username);

        return ResponseEntity.ok(Map.of(
                "message", "Spotify account disconnected successfully",
                "spotifyConnected", false
        ));
    }

    @PutMapping("/user/{username}/referred-by")
    public ResponseEntity<?> updateReferredBy(
            @PathVariable String username,
            @RequestParam String referredByUsername,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        // Ensure user is authenticated and updating their own referredBy
        if (userDetails == null || !userDetails.getUsername().equals(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "You can only update your own referredBy field."));
        }

        try {
            AppUser updatedUser = userService.updateReferredBy(username, referredByUsername);
            return ResponseEntity.ok(mapToUserDto(updatedUser));
        } catch (IllegalArgumentException e) {
            // Handles self-referral case
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            // Handles other errors, e.g., referred username not found
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/referral-leaderboard")
    public List<ReferralDto> getReferralLeaderboard() {
        return userService.getReferralLeaderboard();
    }
}
