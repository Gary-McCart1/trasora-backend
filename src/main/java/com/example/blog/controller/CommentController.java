package com.example.blog.controller;

import com.example.blog.dto.CommentDto;
import com.example.blog.entity.Comment;
import com.example.blog.service.CommentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/comments")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }
    @GetMapping
    public ResponseEntity<List<CommentDto>> getComments(){
        List<Comment> comments = commentService.getComments();
        List<CommentDto> commentDtos = comments.stream()
                .map(commentService::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(commentDtos);
    }

    @PostMapping
    public ResponseEntity<CommentDto> createComment(
            @RequestParam Long postId,
            @RequestBody String commentText
    ) {
        CommentDto createdComment = commentService.createComment(postId, commentText);
        return ResponseEntity.ok(createdComment);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Comment> getComment(@PathVariable Long id){
        return ResponseEntity.ok(commentService.getComment(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Comment> editComment(@PathVariable Long id, @RequestBody CommentDto commentDto){
        return ResponseEntity.ok(commentService.editComment(id, commentDto));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Comment> deleteComment(@PathVariable Long id){
        commentService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/post/{id}")
    public ResponseEntity<List<Comment>> getCommentsByPost(@PathVariable Long id){
        return ResponseEntity.ok(commentService.getCommentByPost(id));
    }
}
