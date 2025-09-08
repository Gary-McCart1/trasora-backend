package com.example.blog.controller;

import com.example.blog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/auth/spotify")
@RequiredArgsConstructor
public class SpotifyAuthController {

    private static final Logger logger = LoggerFactory.getLogger(SpotifyAuthController.class);

    private final UserService userService;

    @Value("${spotify.client.id}")
    private String clientId;

    @Value("${spotify.client.secret}")
    private String clientSecret;

    @Value("${spotify.redirect.uri}")
    private String redirectUri;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Start Spotify OAuth login.
     * Pass your app username in 'state' query param.
     */
    @GetMapping("/login")
    public ResponseEntity<Void> login(@RequestParam(required = false) String state) {
        System.out.println(redirectUri);
        String scopes = "user-read-email user-read-private streaming playlist-modify-public playlist-modify-private "
                + "user-read-playback-state user-modify-playback-state user-read-currently-playing";


        String authUrl = "https://accounts.spotify.com/authorize"
                + "?client_id=" + clientId
                + "&response_type=code"
                + "&redirect_uri=" + UriUtils.encode(redirectUri, StandardCharsets.UTF_8)
                + "&scope=" + UriUtils.encode(scopes, StandardCharsets.UTF_8)
                + "&show_dialog=true"; // force Spotify to always ask user to authorize

        if (state != null && !state.isEmpty()) {
            authUrl += "&state=" + UriUtils.encode(state, StandardCharsets.UTF_8);
        }

        logger.info("Redirecting to Spotify authorize URL with state: {}", state);

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, authUrl)
                .build();
    }


    /**
     * Spotify OAuth callback.
     * Extract 'state' param to get your app username, then redirect there.
     */
    @GetMapping("/callback")
    public ResponseEntity<?> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String state) {

        System.out.println(redirectUri);
        logger.info("==== SPOTIFY CALLBACK HIT ====");
        logger.info("Authorization code: {}", code);
        logger.info("Error param: {}", error);
        logger.info("State param (username): {}", state);

        if (error != null) {
            logger.error("Spotify authorization failed: {}", error);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Authorization failed", "details", error));
        }

        if (code == null) {
            logger.error("Missing authorization code in callback");
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Missing authorization code"));
        }

        try {
            // Prepare token request to Spotify
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            String auth = clientId + ":" + clientSecret;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            headers.set(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "authorization_code");
            body.add("code", code);
            body.add("redirect_uri", redirectUri);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://accounts.spotify.com/api/token",
                    request,
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                logger.error("Failed to retrieve Spotify tokens: {}", response.getBody());
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(Map.of("error", "Failed to retrieve Spotify tokens"));
            }

            Map<String, Object> tokenResponse = response.getBody();
            String accessToken = (String) tokenResponse.get("access_token");
            String refreshToken = (String) tokenResponse.get("refresh_token");

            boolean isPremium = false;

            // Check Spotify profile to see if user has premium
            if (accessToken != null) {
                HttpHeaders profileHeaders = new HttpHeaders();
                profileHeaders.setBearerAuth(accessToken);
                HttpEntity<Void> profileRequest = new HttpEntity<>(profileHeaders);

                ResponseEntity<Map> profileResponse = restTemplate.exchange(
                        "https://api.spotify.com/v1/me",
                        HttpMethod.GET,
                        profileRequest,
                        Map.class
                );

                if (profileResponse.getStatusCode().is2xxSuccessful() && profileResponse.getBody() != null) {
                    String product = (String) profileResponse.getBody().get("product");
                    isPremium = "premium".equalsIgnoreCase(product);
                    logger.info("Spotify product for user {}: {}", state, product);
                } else {
                    logger.warn("Failed to fetch Spotify profile for user: {}", state);
                }
            }

            // Save access, refresh tokens, and premium flag to user's AppUser
            if (state != null && !state.isEmpty()) {
                userService.updateSpotifyConnection(state, true, accessToken, refreshToken, isPremium);
                logger.info("Spotify connection updated for user: {} (premium: {})", state, isPremium);
            } else {
                logger.warn("No username (state) provided, skipping Spotify connection update.");
            }

            // Set cookie for frontend session (access token only)
            ResponseCookie cookie = ResponseCookie.from("spotify_token", accessToken)
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(Duration.ofHours(1))
                    .sameSite("None")
                    .build();

            String username = (state != null && !state.isEmpty()) ? state : "defaultUser";
            String frontendBaseUrl = "https://trasora-frontend-web.vercel.app"; // make configurable if needed
            String redirectUrl = frontendBaseUrl + "/profile/" + username;

            HttpHeaders redirectHeaders = new HttpHeaders();
            redirectHeaders.add(HttpHeaders.SET_COOKIE, cookie.toString());
            redirectHeaders.setLocation(URI.create(redirectUrl));

            return new ResponseEntity<>(redirectHeaders, HttpStatus.FOUND);

        } catch (Exception e) {
            logger.error("Exception during Spotify token exchange", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Exception during Spotify token exchange", "details", e.getMessage()));
        }
    }



    /**
     * Optional test endpoint to verify callback handling.
     */
    @GetMapping("/test-callback")
    public ResponseEntity<String> testCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state) {
        logger.info("Test callback hit with code={}, state={}", code, state);
        return ResponseEntity.ok("Test callback received");
    }

}
