package com.example.blog.controller;

import com.example.blog.dto.PostDto;
import com.example.blog.dto.TrunkDto;
import com.example.blog.entity.AppUser;
import com.example.blog.repository.UserRepository;
import com.example.blog.service.PostService;
import com.example.blog.service.TrunkService;
import com.example.blog.service.SpotifyAuthService;
import com.example.blog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/spotify")
@RequiredArgsConstructor
public class SpotifyController {

    private static final Logger logger = LoggerFactory.getLogger(SpotifyController.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final PostService postService;
    private final UserRepository userRepository;
    private final TrunkService trunkService;
    private final SpotifyAuthService spotifyAuthService;
    private final UserService userService;

    // --- Explore ---
    // --- Explore ---
    @GetMapping("/explore")
    public ResponseEntity<?> getExploreContent() {
        Map<String, Object> exploreData = new HashMap<>();
        try {
            // Use global Spotify account for explore page
            String globalAccessToken = spotifyAuthService.getAccessToken(); // NEW METHOD
            Map<String, Object> rawData = fetchExploreData(globalAccessToken);

            List<Map<String, Object>> featuredTracks =
                    (List<Map<String, Object>>) rawData.getOrDefault("featuredTracks", Collections.emptyList());
            Collections.shuffle(featuredTracks);
            exploreData.put("featuredTracks", featuredTracks.subList(0, Math.min(20, featuredTracks.size())));

            List<Map<String, Object>> newReleases =
                    (List<Map<String, Object>>) rawData.getOrDefault("newReleases", Collections.emptyList());
            Collections.shuffle(newReleases);
            exploreData.put("newReleases", newReleases.subList(0, Math.min(20, newReleases.size())));

            return ResponseEntity.ok(exploreData);
        } catch (Exception e) {
            logger.error("Failed to fetch explore data", e);
            exploreData.put("featuredTracks", Collections.emptyList());
            exploreData.put("newReleases", Collections.emptyList());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exploreData);
        }
    }


    private Map<String, Object> fetchExploreData(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        Map<String, Object> exploreData = new HashMap<>();
        Random rand = new Random();

        // Featured tracks
        try {
            int offset = rand.nextInt(50);
            String searchUrl = "https://api.spotify.com/v1/search?q=year:2025&type=track&limit=50&offset=" + offset;
            ResponseEntity<Map> searchResponse =
                    restTemplate.exchange(searchUrl, HttpMethod.GET, entity, Map.class);

            List<Map<String, Object>> tracks =
                    (List<Map<String, Object>>) ((Map<String, Object>) searchResponse.getBody().get("tracks")).get("items");

            Collections.shuffle(tracks);
            exploreData.put("featuredTracks", tracks.subList(0, Math.min(20, tracks.size())));
        } catch (Exception e) {
            logger.warn("Failed to fetch top tracks: {}", e.getMessage());
            exploreData.put("featuredTracks", Collections.emptyList());
        }

        // New releases
        try {
            int offset = rand.nextInt(50);
            String url = "https://api.spotify.com/v1/browse/new-releases?country=US&limit=20&offset=" + offset;
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            Map<String, Object> albumsObj = (Map<String, Object>) response.getBody().get("albums");
            List<Map<String, Object>> albums = (List<Map<String, Object>>) albumsObj.get("items");

            Collections.shuffle(albums);
            exploreData.put("newReleases", albums.subList(0, Math.min(20, albums.size())));
        } catch (Exception e) {
            logger.warn("Failed to fetch new releases: {}", e.getMessage());
            exploreData.put("newReleases", Collections.emptyList());
        }

        return exploreData;
    }

    // --- Albums ---
    @GetMapping("/albums/{albumId}/tracks")
    public ResponseEntity<?> getAlbumTracks(@PathVariable String albumId) {
        try {
            String accessToken = spotifyAuthService.getAccessToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = "https://api.spotify.com/v1/albums/" + albumId + "/tracks";
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Failed to fetch album tracks", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch album tracks");
        }
    }

    // --- Recommendations from Posts ---
    @GetMapping("/recommendations/from-posts")
    public ResponseEntity<?> getRecommendationsFromPosts() {
        try {
            String accessToken = spotifyAuthService.getAccessToken();

            // ✅ Get current user
            AppUser currentUser = userService.getCurrentUser();

            // ✅ Fetch posts by this user (adjust service method accordingly)
            List<PostDto> posts = postService.getPosts(currentUser);

            Set<String> artistNames = posts.stream()
                    .map(PostDto::getArtistName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            if (artistNames.isEmpty()) {
                return ResponseEntity.ok(Collections.emptyList());
            }

            List<Map<String, Object>> recommendations = new ArrayList<>();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            for (String artist : artistNames) {
                try {
                    String query = URLEncoder.encode("artist:" + artist, StandardCharsets.UTF_8);
                    String url = "https://api.spotify.com/v1/search?q=" + query + "&type=track&limit=5";

                    ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
                    Map<String, Object> body = response.getBody();

                    if (body != null && body.containsKey("tracks")) {
                        recommendations.addAll((List<Map<String, Object>>)
                                ((Map<String, Object>) body.get("tracks")).get("items"));
                    }
                } catch (Exception e) {
                    logger.warn("Failed to fetch tracks for artist {}: {}", artist, e.getMessage());
                }
            }

            Collections.shuffle(recommendations);
            List<Map<String, Object>> limited = recommendations.subList(0, Math.min(20, recommendations.size()));

            return ResponseEntity.ok(limited);
        } catch (Exception e) {
            logger.error("Failed to fetch recommendations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch recommendations");
        }
    }


    // --- Track by ID ---
    @GetMapping("/tracks/{trackId}")
    public ResponseEntity<?> getTrackById(@PathVariable String trackId) {
        try {
            String accessToken = spotifyAuthService.getAccessToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = "https://api.spotify.com/v1/tracks/" + trackId;
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Failed to fetch track", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch track");
        }
    }

    // --- Search ---
    @GetMapping("/search")
    public ResponseEntity<?> searchSpotify(@RequestParam String q) {
        try {
            String accessToken = spotifyAuthService.getAccessToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = "https://api.spotify.com/v1/search?q=" + URLEncoder.encode(q, StandardCharsets.UTF_8)
                    + "&type=track&limit=5";
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Failed to search Spotify", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Spotify API error");
        }
    }

    // --- Trunk to Playlist (server account only!) ---
    @PostMapping("/trunk-playlist/{trunkId}")
    public ResponseEntity<?> createPlaylistFromTrunk(@PathVariable Long trunkId) {
        try {
            String accessToken = spotifyAuthService.getAccessToken();

            TrunkDto trunk = trunkService.getTrunk(trunkId);
            if (trunk.getBranches().isEmpty()) {
                return ResponseEntity.badRequest().body("Trunk has no branches to add.");
            }

            List<String> uris = trunk.getBranches().stream()
                    .map(b -> "spotify:track:" + b.getTrackId())
                    .toList();

            Map<String, Object> playlistBody = new HashMap<>();
            playlistBody.put("name", trunk.getName());
            playlistBody.put("description", trunk.getDescription() != null ? trunk.getDescription() : "");
            playlistBody.put("public", trunk.isPublicFlag());

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> createEntity = new HttpEntity<>(playlistBody, headers);
            String createPlaylistUrl = "https://api.spotify.com/v1/me/playlists";
            ResponseEntity<Map> playlistResponse = restTemplate.postForEntity(createPlaylistUrl, createEntity, Map.class);

            if (!playlistResponse.getStatusCode().is2xxSuccessful() || playlistResponse.getBody() == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create playlist.");
            }

            String playlistId = (String) playlistResponse.getBody().get("id");

            Map<String, Object> addTracksBody = Map.of("uris", uris);
            HttpEntity<Map<String, Object>> addTracksEntity = new HttpEntity<>(addTracksBody, headers);

            String addTracksUrl = "https://api.spotify.com/v1/playlists/" + playlistId + "/tracks";
            restTemplate.postForEntity(addTracksUrl, addTracksEntity, Map.class);

            return ResponseEntity.ok(Map.of(
                    "playlistId", playlistId,
                    "playlistUrl", "https://open.spotify.com/playlist/" + playlistId
            ));
        } catch (Exception e) {
            logger.error("Failed to create playlist", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating playlist.");
        }
    }

    // --- Play & Pause ---
    @PostMapping("/play-track")
    public ResponseEntity<?> playTrack(@RequestBody Map<String, String> body) {
        try {
            String trackId = body.get("trackId");
            if (trackId == null) return ResponseEntity.badRequest().body("trackId required");

            String accessToken = spotifyAuthService.getAccessToken();
            String deviceId = getActiveDeviceId(accessToken);

            Map<String, Object> playBody = Map.of("uris", List.of("spotify:track:" + trackId));
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> playEntity = new HttpEntity<>(playBody, headers);
            restTemplate.exchange("https://api.spotify.com/v1/me/player/play?device_id=" + deviceId,
                    HttpMethod.PUT, playEntity, Void.class);

            return ResponseEntity.ok(Map.of("message", "Playback started on device " + deviceId));
        } catch (Exception e) {
            logger.error("Failed to play track", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to play track");
        }
    }

    @PostMapping("/pause-track")
    public ResponseEntity<?> pauseTrack() {
        try {
            String accessToken = spotifyAuthService.getAccessToken();
            String deviceId = getActiveDeviceId(accessToken);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            restTemplate.exchange("https://api.spotify.com/v1/me/player/pause?device_id=" + deviceId,
                    HttpMethod.PUT, entity, Void.class);

            return ResponseEntity.ok(Map.of("message", "Playback paused on device " + deviceId));
        } catch (Exception e) {
            logger.error("Failed to pause track", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to pause track");
        }
    }

    private String getActiveDeviceId(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String devicesUrl = "https://api.spotify.com/v1/me/player/devices";
        Map devicesResponse = restTemplate.exchange(devicesUrl, HttpMethod.GET, entity, Map.class).getBody();
        List<Map<String, Object>> devices = (List<Map<String, Object>>) devicesResponse.get("devices");

        if (devices == null || devices.isEmpty()) {
            throw new RuntimeException("No active Spotify device found. Open Spotify on a device first.");
        }

        return devices.stream()
                .filter(d -> Boolean.TRUE.equals(d.get("is_active")))
                .map(d -> (String) d.get("id"))
                .findFirst()
                .orElse((String) devices.get(0).get("id"));
    }
}
