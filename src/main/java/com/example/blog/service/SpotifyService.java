package com.example.blog.service;

import com.example.blog.dto.SpotifyTrack;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SpotifyService {

    private final UserService userService; // to get the current user's Spotify token

    public SpotifyTrack getTrack(String trackId) {
        // 1. Get the current user's Spotify access token
        String accessToken = userService.getSpotifyToken(userService.getCurrentUser().getUsername())
                .orElseThrow(() -> new RuntimeException("Spotify not connected"));

        // 2. Call Spotify API: GET https://api.spotify.com/v1/tracks/{trackId}
        // 3. Map response to SpotifyTrack DTO
        // For now, return placeholder
        SpotifyTrack track = new SpotifyTrack();
        track.setName("Unknown Title");
        track.setArtistNames("Unknown Artist");
        track.setAlbumImageUrl("https://via.placeholder.com/150");
        return track;
    }
}

