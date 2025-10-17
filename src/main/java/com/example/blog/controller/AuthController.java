package com.example.blog.controller;

import com.example.blog.dto.*;
import com.example.blog.entity.AppUser;
import com.example.blog.repository.UserRepository;
import com.example.blog.service.BlockService;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
                          UserService userService, PasswordEncoder passwordEncoder,
                          S3Service s3Service, UserRepository userRepository, BlockService blockService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.s3Service = s3Service;
        this.userRepository = userRepository;
    }

    /** Utility: extract token from header */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new RuntimeException("Unauthorized: Invalid or missing token");
    }

    /** Utility: validate & resolve current user */
    public AppUser authenticateRequest(HttpServletRequest request) {
        String token = extractToken(request);
        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("Unauthorized: Invalid or missing token");
        }
        String username = jwtUtil.extractUsername(token);
        AppUser user = userService.findByUsernameOrEmail(username);
        if (user == null) throw new RuntimeException("User not found");
        return user;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        try {
            AppUser user = authenticateRequest(request);
            if(user.isBanned()){
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You have been banned for violating our terms of service. Please email trasoramusic@gmail.com to file a claim to be unbanned.");
            }
            return ResponseEntity.ok(mapToUserDto(user));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getLogin(), request.getPassword())
            );
            org.springframework.security.core.userdetails.User userDetails =
                    (org.springframework.security.core.userdetails.User) authentication.getPrincipal();

            AppUser user = userService.findByUsernameOrEmail(userDetails.getUsername());
            if (!user.isVerified()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Please verify your email before logging in.");
            }
            if(user.isBanned()){
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You have been banned for violating our terms of service. Please email trasoramusic@gmail.com to file a claim to be unbanned.");
            }

            // Generate both tokens
            String accessToken = jwtUtil.generateAccessToken(user.getUsername());
            String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("accessToken", accessToken);
            responseData.put("refreshToken", refreshToken);
            responseData.put("user", mapToUserDto(user));

            return ResponseEntity.ok(responseData);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid username or password"));
        }
    }

    /** 🔄 Refresh endpoint */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        if (refreshToken == null || !jwtUtil.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid or expired refresh token");
        }

        // ✅ Make sure it's actually a refresh token
        if (!"refresh".equals(jwtUtil.extractType(refreshToken))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid token type");
        }

        String username = jwtUtil.extractUsername(refreshToken);

        String newAccessToken = jwtUtil.generateAccessToken(username);
        String newRefreshToken = jwtUtil.generateRefreshToken(username);

        return ResponseEntity.ok(Map.of(
                "accessToken", newAccessToken,
                "refreshToken", newRefreshToken
        ));
    }



    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody RegisterRequest authRequest) {
        AppUser newUser = userService.registerUser(authRequest.getEmail(), authRequest.getUsername(),
                authRequest.getPassword(), authRequest.getFullName());

        String token = userService.createVerificationToken(newUser);
        userService.sendVerificationEmail(newUser.getEmail(), token);

        return ResponseEntity.ok("User registered successfully. Please check your email to verify your account.");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("None")
                .maxAge(0)
                .build();

        response.setHeader("Set-Cookie", cookie.toString());
        return ResponseEntity.ok("Logged out successfully");
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getUsers() {
        List<AppUser> users = userService.getUsers();
        return ResponseEntity.ok(users.stream().map(this::mapToUserDto).toList());
    }

    @DeleteMapping("/user/{username}")
    public ResponseEntity<?> deleteUser(@PathVariable String username, HttpServletRequest request) {
        try {
            AppUser currentUser = authenticateRequest(request);
            if (!currentUser.getUsername().equals(username)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can only delete your own account.");
            }
            userService.deleteUser(username);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<UserDto> getUserByUsername(@PathVariable String username) {
        AppUser user = userService.getUserByUsername(username);
        return user == null
                ? ResponseEntity.status(HttpStatus.NOT_FOUND).build()
                : ResponseEntity.ok(mapToUserDto(user));
    }

    @PutMapping(value = "/user", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> editUserProfile(
            HttpServletRequest request,
            @RequestPart(value = "bio", required = false) String bio,
            @RequestPart(value = "referredBy", required = false) String referredUsername,
            @RequestPart(value = "profilePic", required = false) MultipartFile profilePic,
            @RequestPart(value = "accentColor", required = false) String accentColor) throws IOException {

        try {
            AppUser currentUser = authenticateRequest(request);

            String profilePicUrl = null;
            if (profilePic != null && !profilePic.isEmpty()) {
                profilePicUrl = s3Service.uploadFile(
                        "profile-pictures/" + System.currentTimeMillis() + "-" + profilePic.getOriginalFilename(),
                        profilePic.getInputStream(),
                        profilePic.getSize(),
                        profilePic.getContentType());
            }
            AppUser referredByUser = null;
            if (referredUsername != null) {
                if (referredUsername.equals(currentUser.getUsername())){
                    throw new RuntimeException("You cannot refer yourself.");
                } else {
                    referredByUser = userRepository.findByUsername(referredUsername)
                            .orElseThrow(() -> new RuntimeException("Referrer not found"));
                }

            }

            AppUser updatedUser = userService.editUser(
                    currentUser.getUsername(),
                    bio,
                    profilePicUrl,
                    accentColor, referredByUser);

            return ResponseEntity.ok(mapToUserDto(updatedUser));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @PutMapping("/{username}/profile-visibility")
    public ResponseEntity<?> updateProfileVisibility(
            @PathVariable String username,
            @RequestParam boolean profilePublic,
            HttpServletRequest request) {
        try {
            AppUser currentUser = authenticateRequest(request);
            if (!currentUser.getUsername().equals(username)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            AppUser updatedUser = userService.updateProfileVisibility(username, profilePublic);
            return ResponseEntity.ok(mapToUserDto(updatedUser));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @PutMapping("/user/{username}/disconnect-spotify")
    public ResponseEntity<?> disconnectSpotify(
            @PathVariable String username,
            HttpServletRequest request) {
        try {
            AppUser currentUser = authenticateRequest(request);
            if (!currentUser.getUsername().equals(username)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You can only disconnect your own Spotify account."));
            }
            userService.disconnectSpotify(username);
            return ResponseEntity.ok(Map.of("message", "Spotify account disconnected", "spotifyConnected", false));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @PutMapping("/user/{username}/referred-by")
    public ResponseEntity<?> updateReferredBy(
            @PathVariable String username,
            @RequestParam String referredByUsername,
            HttpServletRequest request) {
        try {
            AppUser currentUser = authenticateRequest(request);
            if (!currentUser.getUsername().equals(username)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You can only update your own referredBy field."));
            }
            AppUser updatedUser = userService.updateReferredBy(username, referredByUsername);
            return ResponseEntity.ok(mapToUserDto(updatedUser));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/referral-leaderboard")
    public List<ReferralDto> getReferralLeaderboard() {
        return userService.getReferralLeaderboard();
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchUsers(@RequestParam String q) {
        List<AppUser> users = userService.searchUsersByUsername(q, 5);
        return ResponseEntity.ok(Map.of("users", users));
    }

    @GetMapping("/search-bar")
    public ResponseEntity<Map<String, Object>> searchBarUsers(@RequestParam String q) {
        List<UserDto> userDtos = userService.searchUsersByUsername(q, 5).stream()
                .map(this::mapToUserDto).toList();
        return ResponseEntity.ok(Map.of("users", userDtos));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam String token) {
        Optional<AppUser> userOpt = userService.findByVerificationToken(token);

        if (userOpt.isEmpty()) {
            // Instead of just saying invalid, check if it's already verified
            Optional<AppUser> alreadyVerified = userService.findByTokenEvenIfNull(token);
            if (alreadyVerified.isPresent() && alreadyVerified.get().isVerified()) {
                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", "Email already verified"
                ));
            }
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Invalid or expired verification token"
            ));
        }

        AppUser user = userOpt.get();
        if (user.isVerified()) {
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Email already verified"
            ));
        }

        user.setVerified(true);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Email verified successfully"
        ));
    }


    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        try {
            boolean sent = userService.sendPasswordResetEmail(email);
            if (!sent) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "No account found for email: " + email));
            }
            return ResponseEntity.ok(Map.of("message", "Password reset email sent if the account exists."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "An error occurred. Try again later."));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest req) {
        userService.resetPassword(req.getToken(), req.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        try {
            AppUser user = userService.findByUsernameOrEmail(email);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "User not found"));
            }
            if (user.isVerified()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "User already verified"));
            }
            String token = userService.createVerificationToken(user);
            userService.sendVerificationEmail(user.getEmail(), token);
            return ResponseEntity.ok(Map.of("message", "Verification email sent"));
        } catch (Exception e) {
            e.printStackTrace(); // <--- log the real exception
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Internal server error: " + e.getMessage()));
        }
    }


    /** --- Mappers --- */
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
                user.getReferredBy() != null && !user.getReferredBy().isBlank()
                        ? user.getReferredBy()
                        : null,
                user.getPushSubscriptionEndpoint(),
                user.getPushSubscriptionKeysAuth(),
                user.getPushSubscriptionKeysP256dh(),
                user.getApnDeviceToken(),
                user.isBanned()
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


}