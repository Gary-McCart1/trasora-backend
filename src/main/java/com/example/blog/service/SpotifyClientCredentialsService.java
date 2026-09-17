package com.example.blog.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Service
public class SpotifyClientCredentialsService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spotify.client.id}")
    private String clientId;

    @Value("${spotify.client.secret}")
    private String clientSecret;

    private String cachedAccessToken;
    private long tokenExpiryTime = 0;

    public synchronized String getAccessToken() {
        long now = System.currentTimeMillis();

        if (cachedAccessToken == null || now >= tokenExpiryTime) {
            refreshAccessToken();
        }

        return cachedAccessToken;
    }

    private void refreshAccessToken() {

        String url = "https://accounts.spotify.com/api/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(
                MediaType.APPLICATION_FORM_URLENCODED
        );

        String auth = clientId + ":" + clientSecret;

        String encodedAuth = Base64.getEncoder()
                .encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        headers.set(
                "Authorization",
                "Basic " + encodedAuth
        );

        MultiValueMap<String, String> body =
                new LinkedMultiValueMap<>();

        body.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        request,
                        Map.class
                );

        Map<String, Object> responseBody = response.getBody();

        if (responseBody == null ||
                !responseBody.containsKey("access_token")) {

            throw new RuntimeException(
                    "Failed to obtain Spotify client credentials token"
            );
        }

        cachedAccessToken =
                (String) responseBody.get("access_token");

        int expiresIn =
                ((Number) responseBody.get("expires_in")).intValue();

        tokenExpiryTime =
                System.currentTimeMillis()
                        + Math.max(60, expiresIn - 60) * 1000L;
    }
}
