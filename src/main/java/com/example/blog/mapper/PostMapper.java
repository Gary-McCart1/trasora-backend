package com.example.blog.mapper;

import com.example.blog.dto.CommentDto;
import com.example.blog.dto.PostDto;
import com.example.blog.entity.AppUser;
import com.example.blog.entity.Post;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class PostMapper {

    public static PostDto toDto(Post post, AppUser currentUser) {
        PostDto dto = new PostDto();
        dto.setId(post.getId());
        dto.setAuthorUsername(post.getAuthor() != null ? post.getAuthor().getUsername() : null);
        dto.setAuthorProfilePictureUrl(post.getAuthor() != null ? post.getAuthor().getProfilePictureUrl() : null);
        dto.setAuthorId(post.getAuthor() != null ? post.getAuthor().getId() : null);
        dto.setTitle(post.getTitle());
        dto.setText(post.getText());
        dto.setTrackId(post.getTrackId());
        dto.setTrackName(post.getTrackName());
        dto.setArtistName(post.getArtistName());
        dto.setAlbumArtUrl(post.getAlbumArtUrl());
        dto.setCustomImageUrl(post.getCustomImageUrl());
        dto.setCustomVideoUrl(post.getCustomVideoUrl());
        dto.setTrackVolume(post.getTrackVolume());
        dto.setS3Key(post.getS3Key());
        dto.setCreatedAt(post.getCreatedAt().format(DateTimeFormatter.ISO_DATE_TIME));

        // ✅ Map SoundCloud fields
        dto.setAppleTrackId(post.getAppleTrackId());
        dto.setAppleTrackName(post.getAppleTrackName());
        dto.setAppleArtistName(post.getAppleArtistName());
        dto.setAppleAlbumArtUrl(post.getAppleAlbumArtUrl());
        dto.setApplePreviewUrl(post.getApplePreviewUrl());

        dto.setLikesCount(post.getLikedBy() != null ? post.getLikedBy().size() : 0);
        dto.setLikedByCurrentUser(currentUser != null && post.isLikedByUser(currentUser));
        dto.setBranchCount(post.getBranchCount());

        List<CommentDto> commentDtos = post.getComments().stream()
                .map(CommentMapper::toDto)
                .collect(Collectors.toList());
        dto.setComments(commentDtos);

        if (post.getAuthor() != null) {
            boolean canView = post.getAuthor().isProfilePublic() ||
                    (currentUser != null && currentUser.getId().equals(post.getAuthor().getId()));
            dto.setPublic(canView);
        } else {
            dto.setPublic(false);
        }

        return dto;
    }
}

