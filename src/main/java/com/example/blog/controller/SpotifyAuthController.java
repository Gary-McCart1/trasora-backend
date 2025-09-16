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

    @GetMapping("/login")
    public ResponseEntity<Void> login(@RequestParam(required = false) String state) {
        String scopes = "user-read-email user-read-private streaming playlist-modify-public playlist-modify-private "
                + "user-read-playback-state user-modify-playback-state user-read-currently-playing";

        System.out.println(scopes);
        String authUrl = "https://accounts.spotify.com/authorize"
                + "?client_id=" + clientId
                + "&response_type=code"
                + "&redirect_uri=" + UriUtils.encode(redirectUri, StandardCharsets.UTF_8)
                + "&scope=" + UriUtils.encode(scopes, StandardCharsets.UTF_8)
                + "&show_dialog=true";

        if (state != null && !state.isEmpty()) {
            authUrl += "&state=" + UriUtils.encode(state, StandardCharsets.UTF_8);
        }

        logger.info("Redirecting to Spotify authorize URL with state: {}", state);

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, authUrl)
                .build();
    }

    @GetMapping("/callback")
    public ResponseEntity<?> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String state) {

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
            // Exchange code for access/refresh tokens
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
                        .body(Map.of("error", "Failed to retrieve Spotify tokens", "details", response.getBody()));
            }

            Map<String, Object> tokenResponse = response.getBody();
            String accessToken = (String) tokenResponse.get("access_token");
            String refreshToken = (String) tokenResponse.get("refresh_token");

            // **LOG THE REFRESH TOKEN HERE**
            logger.info("Spotify REFRESH TOKEN (copy this for Heroku config!): {}", refreshToken);



            boolean isPremium = false;

            // Fetch Spotify profile
            if (accessToken != null) {
                HttpHeaders profileHeaders = new HttpHeaders();
                profileHeaders.setBearerAuth(accessToken);
                HttpEntity<Void> profileRequest = new HttpEntity<>(profileHeaders);

                try {
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
                        logger.error("Failed to fetch Spotify profile. Status={}, Body={}",
                                profileResponse.getStatusCode(),
                                profileResponse.getBody());

                        return ResponseEntity
                                .status(profileResponse.getStatusCode())
                                .body(Map.of(
                                        "error", "Failed to fetch Spotify profile",
                                        "status", profileResponse.getStatusCode().toString(),
                                        "details", profileResponse.getBody()
                                ));
                    }
                } catch (Exception ex) {
                    logger.error("Exception while fetching Spotify profile", ex);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of(
                                    "error", "Exception while fetching Spotify profile",
                                    "details", ex.getMessage()
                            ));
                }
            }

            // Save access, refresh tokens, and premium flag
            if (state != null && !state.isEmpty()) {
                userService.updateSpotifyConnection(state, true, accessToken, refreshToken, isPremium);
                logger.info("Spotify connection updated for user: {} (premium: {})", state, isPremium);
            } else {
                logger.warn("No username (state) provided, skipping Spotify connection update.");
            }

            // Set cookie for frontend session
            ResponseCookie cookie = ResponseCookie.from("spotify_token", accessToken)
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(Duration.ofHours(1))
                    .sameSite("None")
                    .build();

            String username = (state != null && !state.isEmpty()) ? state : "defaultUser";
            String frontendBaseUrl = "https://trasora-frontend-web.vercel.app";
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

    @GetMapping("/test-callback")
    public ResponseEntity<String> testCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state) {
        logger.info("Test callback hit with code={}, state={}", code, state);
        return ResponseEntity.ok("Test callback received");
    }
}
