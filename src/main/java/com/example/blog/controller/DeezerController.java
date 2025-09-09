package com.example.blog.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/deezer")
public class DeezerController {

    private static final Logger logger = LoggerFactory.getLogger(DeezerController.class);

    private final RestTemplate restTemplate = new RestTemplate();

    // Helper to make Deezer GET requests with proper headers
    private ResponseEntity<Map> deezerGet(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        headers.set("Accept", "application/json");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
    }

    // --- Search for tracks ---
    @GetMapping("/search")
    public ResponseEntity<?> searchDeezer(@RequestParam String q) {
        try {
            String url = "https://api.deezer.com/search?q=" + URLEncoder.encode(q, StandardCharsets.UTF_8);
            ResponseEntity<Map> response = deezerGet(url);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Failed to search Deezer", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Deezer API error");
        }
    }

    // --- Get track by ID ---
    @GetMapping("/tracks/{trackId}")
    public ResponseEntity<?> getTrackById(@PathVariable String trackId) {
        try {
            String url = "https://api.deezer.com/track/" + trackId;
            ResponseEntity<Map> response = deezerGet(url);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Failed to fetch Deezer track", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch Deezer track");
        }
    }

    // --- Get album tracks ---
    @GetMapping("/albums/{albumId}/tracks")
    public ResponseEntity<?> getAlbumTracks(@PathVariable String albumId) {
        try {
            String url = "https://api.deezer.com/album/" + albumId + "/tracks";
            ResponseEntity<Map> response = deezerGet(url);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Failed to fetch Deezer album tracks", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch album tracks");
        }
    }

    // --- Get artist top tracks ---
    @GetMapping("/artists/{artistId}/top")
    public ResponseEntity<?> getArtistTopTracks(@PathVariable String artistId) {
        try {
            String url = "https://api.deezer.com/artist/" + artistId + "/top?limit=10";
            ResponseEntity<Map> response = deezerGet(url);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Failed to fetch Deezer artist top tracks", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch artist top tracks");
        }
    }

    // --- Test endpoint ---
    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("Deezer API is working!");
    }
}
