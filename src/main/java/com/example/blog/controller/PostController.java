package com.example.blog.controller;

import com.example.blog.dto.CommentDto;
import com.example.blog.dto.PostDto;
import com.example.blog.entity.AppUser;
import com.example.blog.repository.UserRepository;
import com.example.blog.service.FollowService;
import com.example.blog.service.PostService;
import com.example.blog.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;
    private final UserRepository userRepository;
    private final FollowService followService;

    public PostController(PostService postService, UserRepository userRepository, FollowService followService, UserService userService) {
        this.postService = postService;
        this.userRepository = userRepository;
        this.followService = followService;
    }

    private AppUser getAppUserFromPrincipal(UserDetails principal) {
        if (principal == null) {
            return null;
        }
        return userRepository.findByUsername(principal.getUsername()).orElse(null);
    }

    @GetMapping
    public ResponseEntity<List<PostDto>> getPosts(@AuthenticationPrincipal UserDetails principal) {
        AppUser currentUser = getAppUserFromPrincipal(principal);
        List<PostDto> posts = postService.getPosts(currentUser);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/author/{authorUsername}")
    public ResponseEntity<List<PostDto>> getPostsByAuthor(
            @PathVariable String authorUsername,
            @AuthenticationPrincipal UserDetails principal) {
        AppUser currentUser = getAppUserFromPrincipal(principal);
        AppUser author = userRepository.findByUsername(authorUsername)
                .orElseThrow(() -> new RuntimeException("Author not found"));

        if (!author.isProfilePublic()) {
            boolean isFollower = followService.isFollowing(
                    currentUser != null ? currentUser.getUsername() : null,
                    author.getId()
            );
            if (!isFollower && (currentUser == null || !currentUser.getId().equals(author.getId()))) {
                return ResponseEntity.status(403).build();
            }
        }

        List<PostDto> posts = postService.getPostsByAuthor(authorUsername, currentUser);
        return ResponseEntity.ok(posts);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostDto> createPost(
            @RequestPart("postDto") @Valid PostDto postDto,
            @RequestPart(value = "mediaFile", required = false) MultipartFile mediaFile,
            @AuthenticationPrincipal UserDetails principal
    ) {
        AppUser currentUser = getAppUserFromPrincipal(principal);
        if (currentUser == null) return ResponseEntity.status(403).build();

        postDto.setAuthorUsername(currentUser.getUsername());
        PostDto createdPost = postService.createPost(postDto, mediaFile);
        return ResponseEntity.ok(createdPost);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostDto> getPost(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        AppUser currentUser = getAppUserFromPrincipal(principal);
        PostDto post = postService.getPost(id, currentUser);

        if (!post.isPublic() && (currentUser == null || !followService.isFollowing(currentUser.getUsername(), post.getAuthorId()))) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(post);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostDto> editPost(
            @PathVariable Long id,
            @RequestPart("postDto") @Valid PostDto postDto,
            @RequestPart(value = "mediaFile", required = false) MultipartFile mediaFile,
            @AuthenticationPrincipal UserDetails principal
    ) {
        AppUser currentUser = getAppUserFromPrincipal(principal);
        if (currentUser == null) return ResponseEntity.status(403).build();

        postDto.setAuthorUsername(currentUser.getUsername());
        if (!postDto.getAuthorUsername().equals(currentUser.getUsername())) {
            return ResponseEntity.status(403).build();
        }

        PostDto updatedPost = postService.editPost(id, postDto, mediaFile);
        return ResponseEntity.ok(updatedPost);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        AppUser currentUser = getAppUserFromPrincipal(principal);
        if (currentUser == null) return ResponseEntity.status(403).build();

        postService.deletePost(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<CommentDto> addComment(
            @PathVariable Long postId,
            @RequestBody String commentText,
            @AuthenticationPrincipal UserDetails principal
    ) {
        AppUser currentUser = getAppUserFromPrincipal(principal);
        if (currentUser == null) return ResponseEntity.status(403).build();

        CommentDto comment = postService.addComment(postId, commentText, currentUser);
        return ResponseEntity.ok(comment);
    }


    @PostMapping("/{postId}/like")
    public ResponseEntity<Void> likePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        AppUser currentUser = getAppUserFromPrincipal(principal);
        if (currentUser == null) return ResponseEntity.status(403).build();

        postService.likePost(postId, currentUser);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{postId}/branch")
    public ResponseEntity<Void> branchPost(@PathVariable Long postId,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        AppUser currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found with the username: " + userDetails.getUsername()));
        postService.incrementBranchCount(postId, currentUser);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/feed")
    public List<PostDto> getFeed(@AuthenticationPrincipal UserDetails principal) {
        AppUser currentUser = getAppUserFromPrincipal(principal);
        return postService.getFeedPosts(currentUser);
    }

    @GetMapping("/{authorUsername}/images")
    public List<PostDto> getUserImagePosts(@PathVariable String authorUsername) {
        AppUser user = userRepository.findByUsername(authorUsername).orElseThrow(() -> new RuntimeException("User not found with the username" + authorUsername));

        return postService.getUserImagePosts(user);
    }

    @GetMapping("/{authorUsername}/videos")
    public List<PostDto> getUserVideoPosts(@PathVariable String authorUsername) {
        AppUser user = userRepository.findByUsername(authorUsername).orElseThrow(() -> new RuntimeException("User not found with the username" + authorUsername));
        return postService.getUserVideoPosts(user);
    }
}
