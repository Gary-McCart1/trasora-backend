package com.example.blog.controller;

import com.example.blog.service.AppleMusicTokenService;
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
@RequestMapping("/api/apple-music")
public class AppleMusicController {

    private final AppleMusicTokenService tokenService;
    private final RestTemplate restTemplate = new RestTemplate();

    public AppleMusicController(AppleMusicTokenService tokenService) {
        this.tokenService = tokenService;
    }

    private ResponseEntity<Map> appleMusicGet(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + tokenService.generateDeveloperToken());
        headers.set("Accept", "application/json");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchTracks(@RequestParam String q) {
        try {
            String url = "https://api.music.apple.com/v1/catalog/us/search?term="
                    + URLEncoder.encode(q, StandardCharsets.UTF_8)
                    + "&types=songs&limit=25";
            ResponseEntity<Map> response = appleMusicGet(url);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Apple Music API error: " + e.getMessage());
        }
    }

    @GetMapping("/tracks/{trackId}")
    public ResponseEntity<?> getTrackById(@PathVariable String trackId) {
        try {
            String url = "https://api.music.apple.com/v1/catalog/us/songs/" + trackId;
            ResponseEntity<Map> response = appleMusicGet(url);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Apple Music API error: " + e.getMessage());
        }
    }

    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("Apple Music API is working!");
    }
}
