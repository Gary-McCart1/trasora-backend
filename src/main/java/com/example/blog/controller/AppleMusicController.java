package com.example.blog.controller;

import com.example.blog.entity.Post;
import com.example.blog.entity.Post;
import com.example.blog.repository.PostRepository;
import com.example.blog.service.AppleMusicTokenService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/apple-music")
public class AppleMusicController {

    private final AppleMusicTokenService tokenService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final PostRepository postRepository;

    public AppleMusicController(AppleMusicTokenService tokenService, PostRepository postRepository) {
        this.tokenService = tokenService;
        this.postRepository = postRepository;
    }

    private ResponseEntity<Map> appleMusicGet(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + tokenService.generateDeveloperToken());
        headers.set("Accept", "application/json");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
    }

    @PostMapping("/match/{postId}")
    public ResponseEntity<?> matchSpotifyTrackToPost(
            @PathVariable Long postId,
            @RequestParam String trackName,
            @RequestParam String artistName) {
        try {
            // Search Apple Music
            String query = trackName + " " + artistName;
            String url = "https://api.music.apple.com/v1/catalog/us/search?term="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&types=songs&limit=1";

            ResponseEntity<Map> response = appleMusicGet(url);
            Map body = response.getBody();

            if (body == null || !body.containsKey("results")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No results found in Apple Music");
            }

            Map results = (Map) body.get("results");
            Map songs = (Map) results.get("songs");
            if (songs == null || !songs.containsKey("data")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No songs found for query");
            }

            var data = (List<Map<String, Object>>) songs.get("data");
            if (data.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No song data returned");
            }

            Map<String, Object> song = data.get(0);
            Map<String, Object> attributes = (Map<String, Object>) song.get("attributes");

            String appleTrackId = (String) song.get("id");
            String appleTrackName = (String) attributes.get("name");
            String appleArtistName = (String) attributes.get("artistName");
            String applePreviewUrl = attributes.get("previews") != null && !((List) attributes.get("previews")).isEmpty()
                    ? (String) ((Map) ((List) attributes.get("previews")).get(0)).get("url")
                    : null;
            String appleAlbumArtUrl = attributes.get("artwork") != null
                    ? ((Map<String, Object>) attributes.get("artwork")).get("url").toString()
                    : null;

            // Update Post entity
            Optional<Post> optionalPost = postRepository.findById(postId);
            if (optionalPost.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Post not found");
            }

            Post post = optionalPost.get();
            post.setAppleTrackId(appleTrackId);
            post.setAppleTrackName(appleTrackName);
            post.setAppleArtistName(appleArtistName);
            post.setApplePreviewUrl(applePreviewUrl);
            post.setAppleAlbumArtUrl(appleAlbumArtUrl);

            Post saved = postRepository.save(post);

            return ResponseEntity.ok(saved);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Apple Music API error: " + e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchTracks(@RequestParam String q) {
        try {
            String url = "https://api.music.apple.com/v1/catalog/us/search?term="
                    + URLEncoder.encode(q, StandardCharsets.UTF_8)
                    + "&types=songs&limit=25";
            ResponseEntity<Map> response = appleMusicGet(url);
            return ResponseEntity.ok(response.getBody()); }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Apple Music API error: " + e.getMessage());
        }
    } @GetMapping("/tracks/{trackId}")
    public ResponseEntity<?> getTrackById(@PathVariable String trackId) {
        try {
            String url = "https://api.music.apple.com/v1/catalog/us/songs/" + trackId;
            ResponseEntity<Map> response = appleMusicGet(url);
            return ResponseEntity.ok(response.getBody());
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Apple Music API error: " + e.getMessage());
        }
    }
}
