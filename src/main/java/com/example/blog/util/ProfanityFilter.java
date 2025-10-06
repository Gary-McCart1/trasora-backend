package com.example.blog.util;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class ProfanityFilter {

    // Example word list – expand as needed
    private final List<String> bannedWords = Arrays.asList(
            "ass", "asshole", "bastard", "bitch", "bollocks", "bugger", "cunt",
            "damn", "dick", "douche", "fuck", "fucker", "fucking", "jerk",
            "motherfucker", "nigga", "nigger", "prick", "piss", "shit", "shitty",
            "slut", "twat", "whore", "wanker"
    );

    public boolean containsProfanity(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase();
        return bannedWords.stream().anyMatch(lower::contains);
    }

    public String censor(String text) {
        if (text == null) return null;
        String censored = text;
        for (String word : bannedWords) {
            String replacement = "*".repeat(word.length());
            censored = censored.replaceAll("(?i)" + word, replacement);
        }
        return censored;
    }
}
