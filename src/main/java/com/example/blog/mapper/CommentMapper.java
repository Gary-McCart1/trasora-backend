package com.example.blog.mapper;

import com.example.blog.dto.CommentDto;
import com.example.blog.entity.Comment;

public class CommentMapper {
    public static CommentDto toDto(Comment comment) {
        CommentDto dto = new CommentDto();
        dto.setId(comment.getId());
        dto.setAuthorUsername(comment.getAuthor().getUsername());
        dto.setAuthorProfilePictureUrl(comment.getAuthor().getProfilePictureUrl());
        dto.setCommentText(comment.getCommentText());
        dto.setPostId(comment.getPost().getId());
        dto.setCreatedAt(comment.getCreatedAt());
        return dto;
    }
}

