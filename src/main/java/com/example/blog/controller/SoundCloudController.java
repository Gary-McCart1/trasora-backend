package com.example.blog.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/soundcloud")
public class SoundCloudController {

    private static final Logger logger = LoggerFactory.getLogger(SoundCloudController.class);
    private final RestTemplate restTemplate = new RestTemplate();

    // Add your SoundCloud client ID here or via application.properties
    @Value("${soundcloud.client-id}")
    private String clientId;

    // Helper to make SoundCloud GET requests
    private ResponseEntity<Map> soundCloudGet(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0");
        headers.set("Accept", "application/json");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
    }

    // --- Search tracks ---
    @GetMapping("/search")
    public ResponseEntity<?> searchTracks(@RequestParam String q) {
        try {
            String url = "https://api-v2.soundcloud.com/search/tracks?q="
                    + URLEncoder.encode(q, StandardCharsets.UTF_8)
                    + "&client_id=" + clientId;
            ResponseEntity<Map> response = soundCloudGet(url);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Failed to search SoundCloud tracks", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("SoundCloud API error");
        }
    }

    // --- Get track by ID ---
    @GetMapping("/tracks/{trackId}")
    public ResponseEntity<?> getTrackById(@PathVariable String trackId) {
        try {
            String url = "https://api-v2.soundcloud.com/tracks/" + trackId + "?client_id=" + clientId;
            ResponseEntity<Map> response = soundCloudGet(url);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Failed to fetch SoundCloud track", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch SoundCloud track");
        }
    }

    // --- Test endpoint ---
    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("SoundCloud API is working!");
    }
}
