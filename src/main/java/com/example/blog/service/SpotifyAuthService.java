package com.example.blog.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

@Service
public class SpotifyAuthService {
    private static final Logger logger = LoggerFactory.getLogger(SpotifyAuthService.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spotify.client.id}")
    private String clientId;

    @Value("${spotify.client.secret}")
    private String clientSecret;

    @Value("${spotify.refresh.token}")
    private String refreshToken;

    private String cachedAccessToken;
    private long tokenExpiryTime = 0; // epoch ms

    public String getAccessToken() {

        long now = System.currentTimeMillis();
        if (cachedAccessToken == null || now >= tokenExpiryTime) {
            refreshAccessToken();
        }
        return cachedAccessToken;
    }

    public void refreshAccessToken() {
        String url = "https://accounts.spotify.com/api/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Create the authorization header
        String auth = clientId + ":" + clientSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        headers.set("Authorization", "Basic " + encodedAuth);

        // Prepare the body for the refresh request
        String body = "grant_type=refresh_token&refresh_token=" + refreshToken;
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        try {
            // Send the refresh token request
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            Map<String, Object> responseBody = response.getBody();

            // Check the response
            if (responseBody != null && responseBody.containsKey("access_token")) {
                cachedAccessToken = (String) responseBody.get("access_token");
                int expiresIn = (Integer) responseBody.get("expires_in");

                // Calculate token expiry time (subtract 60 seconds as a buffer)
                tokenExpiryTime = System.currentTimeMillis() + (expiresIn - 60) * 1000L;

                logger.info("Spotify token refreshed successfully. Expires in {} seconds", expiresIn);
            } else {
                // Log the full response for debugging
                logger.error("Failed to refresh token. Response body: {}", responseBody);
                throw new RuntimeException("Failed to refresh Spotify access token");
            }
        } catch (Exception e) {
            logger.error("Error refreshing Spotify access token", e);
            throw new RuntimeException("Error refreshing Spotify access token", e);
        }
    }
}
