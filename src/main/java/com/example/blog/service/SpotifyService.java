package com.example.blog.service;

import com.example.blog.dto.SpotifyTrack;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SpotifyService {

    private static final Logger logger = LoggerFactory.getLogger(SpotifyService.class);

    private final UserService userService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spotify.client.id}")
    private String clientId;

    @Value("${spotify.client.secret}")
    private String clientSecret;

    private String cachedToken;
    private Instant tokenExpiry;

    /**
     * Old behavior: get a track with the current user's Spotify token
     */
    public SpotifyTrack getTrack(String trackId) {
        String accessToken = userService.getSpotifyToken(userService.getCurrentUser().getUsername())
                .orElseThrow(() -> new RuntimeException("Spotify not connected"));

        // TODO: actually call Spotify API using the user token
        SpotifyTrack track = new SpotifyTrack();
        track.setName("Unknown Title");
        track.setArtistNames("Unknown Artist");
        track.setAlbumImageUrl("https://via.placeholder.com/150");
        return track;
    }

    /**
     * New behavior: system-level search using client credentials
     */
    public String searchTrackId(String title, String artist) {
        String token = getAppAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String query = title + " " + artist;
        String url = "https://api.spotify.com/v1/search?q=" + query + "&type=track&limit=1";

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Spotify search failed: " + response.getBody());
        }

        Map<String, Object> body = response.getBody();
        Map tracks = (Map) body.get("tracks");
        List<Map> items = (List<Map>) tracks.get("items");

        if (items.isEmpty()) {
            logger.warn("No Spotify track found for query: {}", query);
            return null;
        }

        return (String) items.get(0).get("id");
    }

    private synchronized String getAppAccessToken() {
        if (cachedToken != null && tokenExpiry != null && Instant.now().isBefore(tokenExpiry)) {
            return cachedToken;
        }

        String auth = clientId + ":" + clientSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://accounts.spotify.com/api/token",
                request,
                Map.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Failed to get Spotify token: " + response.getBody());
        }

        Map<String, Object> respBody = response.getBody();
        cachedToken = (String) respBody.get("access_token");
        Integer expiresIn = (Integer) respBody.get("expires_in");
        tokenExpiry = Instant.now().plusSeconds(expiresIn - 60);

        logger.info("Fetched new Spotify app token, valid for {}s", expiresIn);
        return cachedToken;
    }
}
