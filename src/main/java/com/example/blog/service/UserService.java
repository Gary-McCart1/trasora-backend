package com.example.blog.service;

import com.example.blog.dto.ReferralDto;
import com.example.blog.dto.UserDto;
import com.example.blog.entity.AppUser;
import com.example.blog.repository.FollowRepository;
import com.example.blog.repository.UserRepository;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;
    private final FollowRepository followRepository;
    private final JavaMailSender mailSender;
    private final RestTemplate restTemplate = new RestTemplate();

    @Getter
    @Value("${spotify.client.id}")
    private String spotifyClientId;

    @Getter
    @Value("${spotify.client.secret}")
    private String spotifyClientSecret;

    private static final Pattern EMAIL_REGEX = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       FileStorageService fileStorageService, FollowRepository followRepository,
                       JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.fileStorageService = fileStorageService;
        this.followRepository = followRepository;
        this.mailSender = mailSender;
    }

    // --- User DTO ---
    public UserDto toUserDTO(AppUser user) {
        int followersCount = getFollowersCount(user.getId());
        int followingCount = getFollowingCount(user.getId());

        return new UserDto(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getUsername(),
                user.getBio(),
                user.getJoinedAt(),
                user.getProfilePictureUrl(),
                user.getRole(),
                followersCount,
                followingCount,
                user.getAccentColor(),
                user.isSpotifyConnected(),
                user.isProfilePublic(),
                user.getBranchCount(),
                user.isSpotifyPremium(),
                user.getReferredBy() != null && !user.getReferredBy().isBlank()
                        ? user.getReferredBy()
                        : null
        );
    }

    // --- User retrieval ---
    public AppUser findByUsernameOrEmail(String login) {
        if (isEmail(login)) {
            return userRepository.findByEmail(login)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + login));
        } else {
            return userRepository.findByUsername(login)
                    .orElseThrow(() -> new RuntimeException("User not found with username: " + login));
        }
    }

    private boolean isEmail(String input) {
        return EMAIL_REGEX.matcher(input).matches();
    }

    public AppUser login(String login, String password) {
        AppUser user = findByUsernameOrEmail(login);
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }
        return user;
    }

    public AppUser registerUser(String email, String username, String password, String fullName) {
        if (userRepository.findByUsername(username).isPresent() || userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Username or email already exists");
        }
        AppUser newUser = new AppUser();
        newUser.setFullName(fullName);
        newUser.setEmail(email);
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setJoinedAt(new Date());
        newUser.setAccentColor("#7C3AED");
        newUser.setSpotifyConnected(false);
        newUser.setVerified(false);
        newUser.setBranchCount(0);
        newUser.setSpotifyPremium(false);
        newUser.setReferredBy(null);
        userRepository.save(newUser);
        return newUser;
    }

    public List<AppUser> getUsers() {
        return userRepository.findAll();
    }

    public AppUser getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    public void deleteUser(String username) {
        AppUser deleteUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with the username: " + username));
        userRepository.delete(deleteUser);
    }

    @Transactional
    public AppUser editUser(String username, String bio, String profilePictureUrl, String accentColor, AppUser referredByUsername) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        user.setBio(bio);
        if (accentColor != null) user.setAccentColor(accentColor);
        if (profilePictureUrl != null) user.setProfilePictureUrl(profilePictureUrl);
        if (referredByUsername != null) user.setReferredBy(referredByUsername.getUsername());
        return userRepository.save(user);
    }

    public AppUser getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with the username: " + username));
    }

    public int getFollowersCount(Long userId) {
        return followRepository.countByFollowing_IdAndAcceptedTrue(userId);
    }

    public int getFollowingCount(Long userId) {
        return followRepository.countByFollowerIdAndAcceptedTrue(userId);
    }

    public List<AppUser> searchUsersByUsername(String query, int limit) {
        return userRepository.findTop5ByUsernameContainingIgnoreCase(query);
    }

    public void updateSpotifyConnection(String username, boolean connected, String accessToken, String refreshToken, boolean isPremium) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setSpotifyConnected(connected);
        user.setSpotifyAccessToken(accessToken);
        user.setSpotifyPremium(isPremium);
        if (refreshToken != null) {
            user.setSpotifyRefreshToken(refreshToken);
        }

        userRepository.save(user);
    }

    // --- Spotify disconnect ---
    @Transactional
    public void disconnectSpotify(String username) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setSpotifyAccessToken(null);
        user.setSpotifyRefreshToken(null);
        user.setSpotifyConnected(false);

        userRepository.save(user);
    }


    // --- Email verification ---
    public String createVerificationToken(AppUser user) {
        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        userRepository.save(user);
        return token;
    }

    public void sendVerificationEmail(String email, String token) {
        String url = "https://trasora-frontend-web.vercel.app/verify-email?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Verify your email");
        message.setText("Thank you for registering. Please click the link to verify your account:\n" + url);

        mailSender.send(message);
    }

    public Optional<AppUser> findByVerificationToken(String token) {
        return userRepository.findByVerificationToken(token);
    }

    // --- Password reset ---
    @Transactional
    public boolean sendPasswordResetEmail(String email) {
        Optional<AppUser> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) return false;

        System.out.println("Hello");
        AppUser user = optionalUser.get();
        String token = UUID.randomUUID().toString();
        user.setPasswordResetToken(token);
        user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        String resetLink = "https://trasora-frontend-web.vercel.app/reset-password?token=" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Password Reset Request");
        message.setText("Click the link below to reset your password:\n" + resetLink);
        try {
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace(); // will show the exact SMTP exception
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }


        return true;
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        AppUser user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid password reset token"));

        if (user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Password reset token has expired");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);
    }

    // --- Profile visibility ---
    @Transactional
    public AppUser updateProfileVisibility(String username, boolean profilePublic) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        user.setProfilePublic(profilePublic);
        return userRepository.save(user);
    }

    // --- Spotify token methods ---
    public Optional<String> getSpotifyToken(String username) {
        return userRepository.findByUsername(username)
                .filter(AppUser::isSpotifyConnected)
                .map(AppUser::getSpotifyAccessToken);
    }

    public String refreshSpotifyToken(String username) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getSpotifyRefreshToken() == null) {
            throw new RuntimeException("No refresh token available, user must reconnect Spotify");
        }

        String auth = spotifyClientId + ":" + spotifyClientSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", user.getSpotifyRefreshToken());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://accounts.spotify.com/api/token",
                request,
                Map.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Failed to refresh Spotify token");
        }

        String newAccessToken = (String) response.getBody().get("access_token");
        user.setSpotifyAccessToken(newAccessToken);
        userRepository.save(user);

        return newAccessToken;
    }

    public void incrementBranchCount(String username){
        AppUser user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("No user found with the username: " + username));
        user.setBranchCount(user.getBranchCount() + 1);
        userRepository.save(user);
    }

    public AppUser updateReferredBy(String currentUser, String referredByUsername) {
        AppUser user = userRepository.findByUsername(currentUser)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (user.getUsername().equalsIgnoreCase(referredByUsername)) {
            throw new IllegalArgumentException("You cannot refer yourself. Please try again.");
        }

        AppUser referrer = userRepository.findByUsername(referredByUsername)
                .orElseThrow(() -> new RuntimeException(
                        "User not found with the username: " + referredByUsername
                ));

        user.setReferredBy(referrer.getUsername());
        return userRepository.save(user);
    }

    public List<ReferralDto> getReferralLeaderboard() {
        List<AppUser> users = userRepository.findAll();

        // Initialize map with all users (count 0)
        Map<String, ReferralDto> referralMap = new HashMap<>();
        for (AppUser user : users) {
            referralMap.put(user.getUsername(), new ReferralDto(
                    user.getUsername(),
                    0,
                    user.getProfilePictureUrl()
            ));
        }

        // Count referrals
        for (AppUser user : users) {
            String referrer = user.getReferredBy();
            if (referrer != null && !referrer.isEmpty()) {
                referralMap.get(referrer).incrementCount();
            }
        }

        // Convert to list, sort descending, take top 10
        return referralMap.values().stream()
                .sorted(Comparator.comparingInt(ReferralDto::getReferralCount).reversed())
                .limit(10)
                .collect(Collectors.toList());
    }


}
