package com.example.blog.service;

import com.example.blog.dto.StoryDto;
import com.example.blog.entity.AppUser;
import com.example.blog.entity.Follow;
import com.example.blog.entity.Story;
import com.example.blog.repository.StoryRepository;
import com.example.blog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StoryService {

    private final StoryRepository storyRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;

    public List<StoryDto> getActiveStories(AppUser currentUser) {
        if (currentUser == null) throw new RuntimeException("Current user cannot be null");

        LocalDateTime now = LocalDateTime.now();
        List<AppUser> followedUsers = currentUser.getFollowing()
                .stream()
                .map(Follow::getFollowing)
                .toList();

        List<Story> activeStories = storyRepository.findByExpiresAtAfterOrderByCreatedAtAsc(now);
        List<Story> filteredStories = activeStories.stream()
                .filter(story -> followedUsers.contains(story.getAuthor())
                        || story.getAuthor().equals(currentUser))
                .toList();

        return filteredStories.stream()
                .map(this::convertToDto)
                .toList();
    }

    public List<StoryDto> getUserStories(AppUser user) {
        if (user == null) throw new RuntimeException("User cannot be null");

        LocalDateTime now = LocalDateTime.now();
        List<Story> stories = storyRepository.findByAuthorAndExpiresAtAfterOrderByCreatedAtAsc(user, now);
        return stories.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    /**
     * Create a story for the current user
     * Accepts optional file (image/video) and required track info
     */
    public StoryDto createStory(AppUser currentUser, MultipartFile file, StoryDto storyDto) throws IOException {
        if (currentUser == null) throw new RuntimeException("Current user cannot be null");
        if (storyDto.getTrackId() == null) throw new RuntimeException("Track ID is required");

        String contentUrl = null;
        String s3Key = null;

        Story.StoryType type;

        if (file != null) {
            // Generate a unique key for S3
            s3Key = "stories/" + currentUser.getUsername() + "/" + System.currentTimeMillis() + "-" + file.getOriginalFilename();
            // Upload to S3
            s3Service.uploadFile(s3Key, file.getInputStream(), file.getSize(), file.getContentType());
            contentUrl = "https://" + s3Service.getBucketName() + ".s3.amazonaws.com/" + s3Key;

            // Determine type from MIME
            String contentType = file.getContentType();
            if (contentType != null && contentType.startsWith("video")) {
                type = Story.StoryType.VIDEO;
            } else if (contentType != null && contentType.startsWith("image")) {
                type = Story.StoryType.IMAGE;
            } else {
                throw new RuntimeException("Unsupported file type: " + contentType);
            }
        } else {
            // No file → it's a track-only story
            contentUrl = "";
            type = Story.StoryType.TRACK;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(24);

        Story story = Story.builder()
                .author(currentUser)
                .trackId(storyDto.getTrackId())
                .trackName(storyDto.getTrackName())
                .artistName(storyDto.getArtistName())
                .albumArtUrl(storyDto.getAlbumArtUrl())
                .contentUrl(contentUrl)
                .s3Key(s3Key)
                .type(type)
                .caption(storyDto.getCaption())
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();

        Story savedStory = storyRepository.save(story);
        return convertToDto(savedStory);
    }

    public void markStoryAsViewed(Long storyId, AppUser currentUser) {
        if (currentUser == null) throw new RuntimeException("Current user cannot be null");

        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found: " + storyId));

        story.getViewers().add(currentUser);
        storyRepository.save(story);
    }

    public void deleteStory(Long storyId, AppUser currentUser) {
        if (currentUser == null) {
            throw new RuntimeException("User not authenticated");
        }

        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found: " + storyId));

        if (!story.getAuthor().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You do not have permission to delete this story");
        }

        // Delete S3 file if exists
        if (story.getS3Key() != null && !story.getS3Key().isEmpty()) {
            try {
                s3Service.deleteFile(story.getS3Key());
            } catch (Exception e) {
                System.err.println("Failed to delete S3 file: " + e.getMessage());
                // Not fatal — continue deleting story record
            }
        }

        storyRepository.delete(story);
    }


    private StoryDto convertToDto(Story story) {
        return StoryDto.builder()
                .id(story.getId())
                .authorId(story.getAuthor().getId())
                .authorUsername(story.getAuthor().getUsername())
                .authorProfilePictureUrl(story.getAuthor().getProfilePictureUrl())
                .contentUrl(story.getContentUrl())
                .trackId(story.getTrackId())
                .trackName(story.getTrackName())
                .artistName(story.getArtistName())
                .albumArtUrl(story.getAlbumArtUrl())
                .type(story.getType())
                .caption(story.getCaption())
                .createdAt(story.getCreatedAt())
                .expiresAt(story.getExpiresAt())
                .viewed(false)
                .build();
    }
}
