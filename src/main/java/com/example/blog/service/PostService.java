package com.example.blog.service;

import com.example.blog.dto.CommentDto;
import com.example.blog.dto.PostDto;
import com.example.blog.entity.AppUser;
import com.example.blog.entity.Comment;
import com.example.blog.entity.NotificationType;
import com.example.blog.entity.Post;
import com.example.blog.mapper.PostMapper;
import com.example.blog.repository.CommentRepository;
import com.example.blog.repository.PostRepository;
import com.example.blog.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PostService {

    private static final Logger log = LoggerFactory.getLogger(PostService.class);
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final S3Service s3Service;
    private final NotificationService notificationService;
    private final FollowService followService;
    private final CommentService commentService;

    public PostService(PostRepository postRepository,
                       UserRepository userRepository,
                       CommentRepository commentRepository,
                       S3Service s3Service,
                       NotificationService notificationService,
                       FollowService followService, CommentService commentService) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
        this.s3Service = s3Service;
        this.notificationService = notificationService;
        this.followService = followService;
        this.commentService = commentService;
    }

    private String uploadMediaToS3(MultipartFile mediaFile) throws IOException {
        if (mediaFile == null || mediaFile.isEmpty()) return null;

        String originalFilename = mediaFile.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }

        String folder = mediaFile.getContentType().startsWith("video") ? "post-videos/" : "post-images/";
        String s3Key = folder + UUID.randomUUID() + extension;

        s3Service.uploadFile(s3Key, mediaFile.getInputStream(), mediaFile.getSize(), mediaFile.getContentType());

        return "https://dreamr-user-content.s3.amazonaws.com/" + s3Key;
    }

    /* ------------------------ Post Methods ------------------------ */

    public List<PostDto> getPosts(AppUser currentUser) {
        return postRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(post -> canViewPost(post, currentUser))
                .map(post -> PostMapper.toDto(post, currentUser))
                .collect(Collectors.toList());
    }

    public PostDto getPost(Long id, AppUser currentUser) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found with ID: " + id));

        if (!canViewPost(post, currentUser)) {
            throw new RuntimeException("You are not allowed to view this post");
        }

        return PostMapper.toDto(post, currentUser);
    }

    public List<PostDto> getPostsByAuthor(String authorUsername, AppUser currentUser) {
        AppUser author = userRepository.findByUsername(authorUsername)
                .orElseThrow(() -> new RuntimeException("No user found with username: " + authorUsername));

        if (!author.isProfilePublic() && (currentUser == null || !followService.isFollowing(currentUser.getUsername(), author.getId()) && !currentUser.getId().equals(author.getId()))) {
            throw new RuntimeException("You are not allowed to view this user's posts");
        }

        return postRepository.findByAuthorOrderByCreatedAtDesc(author)
                .stream()
                .map(post -> PostMapper.toDto(post, currentUser))
                .collect(Collectors.toList());
    }

    public PostDto createPost(PostDto postDto, MultipartFile mediaFile) {
        AppUser user = userRepository.findByUsername(postDto.getAuthorUsername())
                .orElseThrow(() -> new RuntimeException("No user found with username: " + postDto.getAuthorUsername()));

        String customImageUrl = null;
        String customVideoUrl = null;
        String s3Key = null;

        if (mediaFile != null && !mediaFile.isEmpty()) {
            try {
                String uploadedUrl = uploadMediaToS3(mediaFile);
                s3Key = uploadedUrl.substring(uploadedUrl.lastIndexOf("/") + 1);

                if (mediaFile.getContentType().startsWith("video")) {
                    customVideoUrl = uploadedUrl;
                } else {
                    customImageUrl = uploadedUrl;
                }
            } catch (IOException e) {
                // Log the exception to get more details on the cause of the failure
                log.error("Error during media file upload: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to upload media file", e);
            }
        }

        Post post = Post.builder()
                .author(user)
                .title(postDto.getTitle())
                .text(postDto.getText())
                .trackId(postDto.getTrackId())
                .trackName(postDto.getTrackName())
                .trackVolume(postDto.getTrackVolume())
                .artistName(postDto.getArtistName())
                .albumArtUrl(postDto.getAlbumArtUrl())

                .deezerTrackId(postDto.getDeezerTrackId())
                .deezerTrackName(postDto.getDeezerTrackName())
                .deezerArtistName(postDto.getDeezerArtistName())
                .deezerAlbumArtUrl(postDto.getDeezerAlbumArtUrl())
                .deezerPreviewUrl(postDto.getDeezerPreviewUrl())

                .customImageUrl(customImageUrl)
                .customVideoUrl(customVideoUrl)
                .s3Key(s3Key)
                .likedBy(new HashSet<>())
                .comments(new ArrayList<>())
                .branchCount(0)
                .build();

        return PostMapper.toDto(postRepository.save(post), user);
    }

    public PostDto editPost(Long id, PostDto postDto, MultipartFile mediaFile) {
        AppUser author = userRepository.findByUsername(postDto.getAuthorUsername())
                .orElseThrow(() -> new RuntimeException("User not found with username: " + postDto.getAuthorUsername()));

        Post editPost = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found with ID: " + id));

        if (!editPost.getAuthor().getId().equals(author.getId())) {
            throw new RuntimeException("You cannot edit another user's post");
        }

        editPost.setTitle(postDto.getTitle());
        editPost.setText(postDto.getText());
        editPost.setTrackId(postDto.getTrackId());
        editPost.setTrackName(postDto.getTrackName());
        editPost.setArtistName(postDto.getArtistName());
        editPost.setAlbumArtUrl(postDto.getAlbumArtUrl());
        editPost.setTrackVolume(postDto.getTrackVolume());

        if (mediaFile != null && !mediaFile.isEmpty()) {
            try {
                String uploadedUrl = uploadMediaToS3(mediaFile);
                String newS3Key = uploadedUrl.substring(uploadedUrl.lastIndexOf("/") + 1);

                if (mediaFile.getContentType().startsWith("video")) {
                    editPost.setCustomVideoUrl(uploadedUrl);
                } else {
                    editPost.setCustomImageUrl(uploadedUrl);
                }
                editPost.setS3Key(newS3Key);
            } catch (IOException e) {
                throw new RuntimeException("Failed to upload media file", e);
            }
        }

        return PostMapper.toDto(postRepository.save(editPost), author);
    }

    public void deletePost(Long id, AppUser currentUser) {
        Post deletePost = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found with ID: " + id));

        if (!deletePost.getAuthor().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You cannot delete another user's post");
        }

        postRepository.delete(deletePost);
    }

    public void likePost(Long postId, AppUser currentUser) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (!canViewPost(post, currentUser)) {
            throw new RuntimeException("Cannot like a post you cannot view");
        }

        Set<AppUser> likedBy = post.getLikedBy();
        boolean alreadyLiked = likedBy.contains(currentUser);

        if (alreadyLiked) {
            likedBy.remove(currentUser); // Unlike
        } else {
            likedBy.add(currentUser); // Like
            if (!post.getAuthor().getId().equals(currentUser.getId())) {
                notificationService.createNotification(
                        post.getAuthor(),
                        currentUser,
                        NotificationType.LIKE,
                        post,
                        null,
                        null,
                        null,
                        null,
                        null
                );
            }
        }

        postRepository.save(post);
    }

    public CommentDto addComment(Long postId, String commentText, AppUser currentUser) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (!canViewPost(post, currentUser)) {
            throw new RuntimeException("Cannot comment on a post you cannot view");
        }

        Comment comment = new Comment();
        comment.setCommentText(commentText);
        comment.setPost(post);
        comment.setAuthor(currentUser);
        commentRepository.save(comment);

        notificationService.createNotification(
                post.getAuthor(),
                currentUser,
                NotificationType.COMMENT,
                post,
                null,
                null,
                null,
                null,
                null
        );

        return commentService.mapToDto(comment);
    }

    /* ------------------------ Helper: Private Post Visibility ------------------------ */

    private boolean canViewPost(Post post, AppUser currentUser) {
        AppUser author = post.getAuthor();
        if (author.isProfilePublic()) return true;
        if (currentUser == null) return false;
        return currentUser.getId().equals(author.getId()) || followService.isFollowing(currentUser.getUsername(), author.getId());
    }

    @Transactional
    public void incrementBranchCount(Long postId, AppUser currentUser) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (!canViewPost(post, currentUser)) {
            throw new RuntimeException("Cannot branch a post you cannot view");
        }

        post.setBranchCount(post.getBranchCount() + 1);

        if (!post.getAuthor().getId().equals(currentUser.getId())) {
            notificationService.createNotification(
                    post.getAuthor(),
                    currentUser,
                    NotificationType.BRANCH_ADDED,
                    post,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        postRepository.save(post);
    }

    public List<PostDto> getUserImagePosts(AppUser currentUser){
        return postRepository.findImagePosts(currentUser)
                .stream()
                .map(post -> PostMapper.toDto(post, currentUser))
                .collect(Collectors.toList());
    }
    public List<PostDto> getUserVideoPosts(AppUser currentUser){
        return postRepository.findVideoPosts(currentUser)
                .stream()
                .map(post -> PostMapper.toDto(post, currentUser))
                .collect(Collectors.toList());
    }

    public List<PostDto> getFeedPosts(AppUser currentUser) {
        return postRepository.findFeedPosts(currentUser)
                .stream()
                .map(post -> PostMapper.toDto(post, currentUser))
                .collect(Collectors.toList());
    }
}