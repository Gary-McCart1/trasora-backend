package com.example.blog.service;

import com.example.blog.dto.CommentDto;
import com.example.blog.entity.AppUser;
import com.example.blog.entity.Comment;
import com.example.blog.entity.NotificationType;
import com.example.blog.entity.Post;
import com.example.blog.repository.CommentRepository;
import com.example.blog.repository.PostRepository;
import com.example.blog.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public CommentService(CommentRepository commentRepository,
                          PostRepository postRepository,
                          UserRepository userRepository,
                          NotificationService notificationService) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    private AppUser getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    public CommentDto mapToDto(Comment comment) {
        CommentDto dto = new CommentDto();
        dto.setId(comment.getId());  // <-- populate the comment ID
        dto.setPostId(comment.getPost().getId());
        dto.setCommentText(comment.getCommentText());
        dto.setAuthorUsername(comment.getAuthor().getUsername());  // <-- populate username
        dto.setAuthorProfilePictureUrl(comment.getAuthor().getProfilePictureUrl()); // <-- populate profile picture
        dto.setCreatedAt(comment.getCreatedAt()); // <-- populate creation timestamp
        return dto;
    }


    public List<CommentDto> mapToDtoList(List<Comment> comments) {
        return comments.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<Comment> getComments() {
        return commentRepository.findAll();
    }

    public Comment getComment(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No comment found with the ID: " + id));
    }

    public CommentDto createComment(Long postId, String commentText) {
        AppUser currentUser = getCurrentUser();

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with the ID: " + postId));

        Comment newComment = Comment.builder()
                .commentText(commentText)
                .post(post)
                .author(currentUser)
                .build();

        Comment savedComment = commentRepository.save(newComment);

        System.out.println("Comment created here");

        return mapToDto(savedComment);
    }


    public Comment editComment(Long id, CommentDto commentDto) {
        Comment editComment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found with the ID: " + id));
        editComment.setCommentText(commentDto.getCommentText());
        return commentRepository.save(editComment);
    }

    public void deleteComment(Long id) {
        commentRepository.delete(commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No comment found with the ID: " + id)));
    }

    public List<Comment> getCommentByPost(Long postId) {
        return commentRepository.getCommentByPostId(postId)
                .orElseThrow(() -> new RuntimeException("No comments found under Post ID: " + postId));
    }
}
