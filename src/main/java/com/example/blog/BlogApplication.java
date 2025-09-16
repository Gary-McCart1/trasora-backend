package com.example.blog;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BlogApplication {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.configure()
				.ignoreIfMissing() // optional
				.load();

		// Spotify
		System.setProperty("spotify.client.id", dotenv.get("SPOTIFY_CLIENT_ID"));
		System.setProperty("spotify.client.secret", dotenv.get("SPOTIFY_CLIENT_SECRET"));
		System.setProperty("spotify.redirect.uri", dotenv.get("SPOTIFY_REDIRECT_URI"));

		// Apple Music
		System.setProperty("apple.music.key", dotenv.get("APPLE_MUSIC_KEY"));
		System.setProperty("apple.music.key-id", dotenv.get("APPLE_MUSIC_KEY_ID"));
		System.setProperty("apple.music.team-id", dotenv.get("APPLE_MUSIC_TEAM_ID"));
		System.setProperty("apple.music.token-expiration", dotenv.get("APPLE_MUSIC_TOKEN_EXPIRATION"));


		SpringApplication.run(BlogApplication.class, args);
	}


}
