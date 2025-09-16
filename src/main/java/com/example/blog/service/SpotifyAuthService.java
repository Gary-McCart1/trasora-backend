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

    private void refreshAccessToken() {
        String url = "https://accounts.spotify.com/api/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String auth = clientId + ":" + clientSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        headers.set("Authorization", "Basic " + encodedAuth);

        String body = "grant_type=refresh_token&refresh_token=" + refreshToken;
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        Map<String, Object> responseBody = response.getBody();
        if (responseBody != null) {
            cachedAccessToken = (String) responseBody.get("access_token");
            int expiresIn = (Integer) responseBody.get("expires_in");
            tokenExpiryTime = System.currentTimeMillis() + (expiresIn - 60) * 1000L;
            logger.info("Spotify token refreshed successfully");
        } else {
            throw new RuntimeException("Failed to refresh Spotify access token");
        }
    }
}
