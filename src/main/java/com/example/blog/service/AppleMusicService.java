package com.example.blog.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AppleMusicService {

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> searchSong(String trackName, String artistName) {
        try {
            String query = URLEncoder.encode(trackName + " " + artistName, StandardCharsets.UTF_8);
            String url = "https://itunes.apple.com/search?term=" + query + "&entity=song&limit=1";

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("results")) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("results");
                if (!results.isEmpty()) {
                    Map<String, Object> song = results.get(0);

                    // build a clean map with just what you want
                    Map<String, Object> appleData = new HashMap<>();
                    appleData.put("appleTrackId", song.get("trackId"));
                    appleData.put("appleTrackName", song.get("trackName"));
                    appleData.put("appleArtistName", song.get("artistName"));
                    appleData.put("appleAlbumArtUrl", song.get("artworkUrl100"));
                    appleData.put("applePreviewUrl", song.get("previewUrl"));

                    return appleData;
                }
            }
        } catch (Exception e) {
            System.out.println("Apple Music search failed: " + e.getMessage());
        }
        return Collections.emptyMap();
    }
}

