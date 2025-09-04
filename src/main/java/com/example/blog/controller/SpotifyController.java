package com.example.blog.controller;

import com.example.blog.dto.PostDto;
import com.example.blog.dto.TrunkDto;
import com.example.blog.entity.AppUser;
import com.example.blog.repository.UserRepository;
import com.example.blog.service.PostService;
import com.example.blog.service.TrunkService;
import com.example.blog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
    private final UserService userService;
    private final PostService postService;
    private final UserRepository userRepository;
    private final TrunkService trunkService;

    @GetMapping("/explore")
    public ResponseEntity<?> getExploreContent(@RequestHeader("Username") String username) {
        Map<String, Object> exploreData = new HashMap<>();
        try {
            String accessToken = userService.refreshSpotifyToken(username);

            Map<String, Object> rawData = fetchExploreDataWithRetry(accessToken, username);

            // Shuffle featured tracks
            List<Map<String, Object>> featuredTracks = (List<Map<String, Object>>) rawData.getOrDefault("featuredTracks", Collections.emptyList());
            Collections.shuffle(featuredTracks);
            exploreData.put("featuredTracks", featuredTracks.subList(0, Math.min(20, featuredTracks.size()))); // pick 20 random tracks

            // Shuffle new releases
            List<Map<String, Object>> newReleases = (List<Map<String, Object>>) rawData.getOrDefault("newReleases", Collections.emptyList());
            Collections.shuffle(newReleases);
            exploreData.put("newReleases", newReleases.subList(0, Math.min(20, newReleases.size()))); // pick 20 random albums

            return ResponseEntity.ok(exploreData);
        } catch (Exception e) {
            logger.error("Failed to fetch explore data", e);
            exploreData.put("featuredTracks", Collections.emptyList());
            exploreData.put("newReleases", Collections.emptyList());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exploreData);
        }
    }

    private Map<String, Object> fetchExploreDataWithRetry(String accessToken, String username) {
        int attempts = 0;
        while (attempts < 2) {
            try {
                return fetchExploreData(accessToken);
            } catch (HttpClientErrorException.Unauthorized e) {
                attempts++;
                logger.warn("Access token expired, refreshing token for username: {}", username);
                accessToken = userService.refreshSpotifyToken(username);
            }
        }
        throw new RuntimeException("Failed to fetch Spotify data after token refresh");
    }

    private Map<String, Object> fetchExploreData(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        Map<String, Object> exploreData = new HashMap<>();
        Random rand = new Random();

        // Fetch Top Tracks (all of 2025, sorted newest first)
        try {
            int offset = rand.nextInt(50); // random offset for search results
            String searchUrl = "https://api.spotify.com/v1/search?q=year:2025&type=track&limit=50&offset=" + offset;
            ResponseEntity<Map> searchResponse = restTemplate.exchange(searchUrl, HttpMethod.GET, entity, Map.class);

            List<Map<String, Object>> tracks = (List<Map<String, Object>>)
                    ((Map<String, Object>) searchResponse.getBody().get("tracks")).get("items");

            List<Map<String, Object>> sortedTracks = tracks.stream()
                    .filter(t -> ((Map<String, Object>) t.get("album")).get("release_date") != null)
                    .sorted((a, b) -> {
                        String dateA = (String) ((Map<String, Object>) a.get("album")).get("release_date");
                        String dateB = (String) ((Map<String, Object>) b.get("album")).get("release_date");
                        return dateB.compareTo(dateA);
                    })
                    .collect(Collectors.toList());

            Collections.shuffle(sortedTracks); // shuffle for extra randomness
            exploreData.put("featuredTracks", sortedTracks.subList(0, Math.min(20, sortedTracks.size())));
        } catch (HttpClientErrorException e) {
            logger.warn("Failed to fetch top tracks: {}", e.getMessage());
            exploreData.put("featuredTracks", Collections.emptyList());
        }

        // Fetch new releases
        try {
            int offset = rand.nextInt(50); // random offset for new releases
            String newReleasesUrl = "https://api.spotify.com/v1/browse/new-releases?country=US&limit=20&offset=" + offset;
            ResponseEntity<Map> newReleasesResponse = restTemplate.exchange(newReleasesUrl, HttpMethod.GET, entity, Map.class);

            Map<String, Object> albumsObj = (Map<String, Object>) newReleasesResponse.getBody().get("albums");
            List<Map<String, Object>> albums = (List<Map<String, Object>>) albumsObj.get("items");

            Collections.shuffle(albums); // shuffle for extra randomness
            exploreData.put("newReleases", albums.subList(0, Math.min(20, albums.size())));
        } catch (HttpClientErrorException e) {
            logger.warn("Failed to fetch new releases: {}", e.getMessage());
            exploreData.put("newReleases", Collections.emptyList());
        }

        return exploreData;
    }


    @GetMapping("/albums/{albumId}/tracks")
    public ResponseEntity<?> getAlbumTracks(
            @RequestHeader("Username") String username,
            @PathVariable String albumId) {
        try {
            String accessToken = userService.refreshSpotifyToken(username);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = "https://api.spotify.com/v1/albums/" + albumId + "/tracks";
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Failed to fetch album tracks", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch album tracks");
        }
    }

    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("Test endpoint is working!");
    }

    @GetMapping("/recommendations/from-posts")
    public ResponseEntity<?> getRecommendationsFromPosts(@AuthenticationPrincipal UserDetails principal) {
        AppUser currentUser = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String accessToken = userService.refreshSpotifyToken(currentUser.getUsername());

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

        // Shuffle recommendations and limit to 20
        Collections.shuffle(recommendations);
        List<Map<String, Object>> limitedRecs = recommendations.subList(0, Math.min(20, recommendations.size()));

        return ResponseEntity.ok(limitedRecs);
    }

    @GetMapping("/tracks/{trackId}")
    public ResponseEntity<?> getTrackById(
            @RequestHeader("Username") String username,
            @PathVariable String trackId) {
        try {
            String accessToken = userService.refreshSpotifyToken(username);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = "https://api.spotify.com/v1/tracks/" + trackId;
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Failed to fetch track", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch track");
        }
    }
    @GetMapping("/search")
    public ResponseEntity<?> searchSpotify(
            @RequestHeader("Username") String username,
            @RequestParam String q) {
        try {
            String accessToken = userService.refreshSpotifyToken(username);

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

    @PostMapping("/trunk-playlist/{trunkId}")
    public ResponseEntity<?> createPlaylistFromTrunk(
            @PathVariable Long trunkId,
            @AuthenticationPrincipal UserDetails principal) {

        try {
            AppUser currentUser = userService.getCurrentUser();
            String accessToken = userService.refreshSpotifyToken(currentUser.getUsername());

            // Fetch trunk with branches
            TrunkDto trunk = trunkService.getTrunk(trunkId); // make sure this includes branches

            if (trunk.getBranches().isEmpty()) {
                return ResponseEntity.badRequest().body("Trunk has no branches to add to playlist.");
            }

            // Map branches to Spotify track URIs
            List<String> uris = trunk.getBranches().stream()
                    .map(b -> "spotify:track:" + b.getTrackId())
                    .toList();

            // Create playlist body
            Map<String, Object> playlistBody = new HashMap<>();
            playlistBody.put("name", trunk.getName());
            playlistBody.put("description", trunk.getDescription() != null ? trunk.getDescription() : "");
            playlistBody.put("public", trunk.isPublicFlag());

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> createEntity = new HttpEntity<>(playlistBody, headers);

            // 1️⃣ Create the playlist
            String createPlaylistUrl = "https://api.spotify.com/v1/me/playlists";
            ResponseEntity<Map> playlistResponse = restTemplate.postForEntity(createPlaylistUrl, createEntity, Map.class);

            if (!playlistResponse.getStatusCode().is2xxSuccessful() || playlistResponse.getBody() == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create Spotify playlist.");
            }

            String playlistId = (String) playlistResponse.getBody().get("id");

            // 2️⃣ Add tracks to the playlist
            Map<String, Object> addTracksBody = Map.of("uris", uris);
            HttpEntity<Map<String, Object>> addTracksEntity = new HttpEntity<>(addTracksBody, headers);

            String addTracksUrl = "https://api.spotify.com/v1/playlists/" + playlistId + "/tracks";
            ResponseEntity<Map> addTracksResponse = restTemplate.postForEntity(addTracksUrl, addTracksEntity, Map.class);

            if (!addTracksResponse.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to add tracks to playlist.");
            }

            return ResponseEntity.ok(Map.of(
                    "playlistId", playlistId,
                    "playlistUrl", "https://open.spotify.com/playlist/" + playlistId
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while creating the playlist.");
        }
    }

    @GetMapping("/token")
    public ResponseEntity<?> getSpotifyToken(@AuthenticationPrincipal UserDetails principal) {
        try {
            String username = principal.getUsername();
            String accessToken = userService.refreshSpotifyToken(username); // force refresh if expired

            return ResponseEntity.ok(Map.of("accessToken", accessToken));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get Spotify token", "details", e.getMessage()));
        }
    }

    @PostMapping("/play-track")
    public ResponseEntity<?> playTrack(@RequestHeader("Username") String username, @RequestBody Map<String, String> body) {
        try {
            String trackId = body.get("trackId");
            if (trackId == null) return ResponseEntity.badRequest().body("trackId is required");

            String accessToken = userService.refreshSpotifyToken(username);
            String deviceId = getActiveDeviceId(accessToken);

            Map<String, Object> playBody = Map.of("uris", List.of("spotify:track:" + trackId), "device_id", deviceId);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> playEntity = new HttpEntity<>(playBody, headers);

            restTemplate.exchange("https://api.spotify.com/v1/me/player/play", HttpMethod.PUT, playEntity, Void.class);

            return ResponseEntity.ok(Map.of("message", "Playback started on device " + deviceId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to play track", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to play track: " + e.getMessage());
        }
    }

    @PostMapping("/pause-track")
    public ResponseEntity<?> pauseTrack(@RequestHeader("Username") String username) {
        try {
            String accessToken = userService.refreshSpotifyToken(username);
            String deviceId = getActiveDeviceId(accessToken);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> pauseEntity = new HttpEntity<>(headers);

            restTemplate.exchange("https://api.spotify.com/v1/me/player/pause?device_id=" + deviceId, HttpMethod.PUT, pauseEntity, Void.class);

            return ResponseEntity.ok(Map.of("message", "Playback paused on device " + deviceId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to pause track", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to pause track: " + e.getMessage());
        }
    }

    // --- Helper method to get active device ---
    private String getActiveDeviceId(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String devicesUrl = "https://api.spotify.com/v1/me/player/devices";
        Map devicesResponse = restTemplate.exchange(devicesUrl, HttpMethod.GET, entity, Map.class).getBody();
        List<Map<String, Object>> devices = (List<Map<String, Object>>) devicesResponse.get("devices");

        if (devices == null || devices.isEmpty()) {
            throw new RuntimeException("No active Spotify device found. Make sure a device is ready.");
        }

        return devices.stream()
                .filter(d -> Boolean.TRUE.equals(d.get("is_active")))
                .map(d -> (String) d.get("id"))
                .findFirst()
                .orElse((String) devices.get(0).get("id"));
    }

}
