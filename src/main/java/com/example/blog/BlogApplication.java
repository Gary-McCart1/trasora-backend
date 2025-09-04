package com.example.blog;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BlogApplication {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.configure()
				.ignoreIfMissing() // optional: ignore if .env is missing
				.load();

		// Set Spring properties manually
		System.setProperty("spotify.client.id", dotenv.get("SPOTIFY_CLIENT_ID"));
		System.setProperty("spotify.client.secret", dotenv.get("SPOTIFY_CLIENT_SECRET"));
		System.setProperty("spotify.redirect.uri", dotenv.get("SPOTIFY_REDIRECT_URI"));
		SpringApplication.run(BlogApplication.class, args);
	}

}
