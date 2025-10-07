package com.example.blog.service;

import com.example.blog.entity.*;
import com.example.blog.repository.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class FlagService {

    private final FlagRepository flagRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final StoryRepository storyRepository;
    private final JavaMailSender mailSender;

    // how many flags before auto-hide
    private static final int FLAG_THRESHOLD = 3;

    public FlagService(FlagRepository flagRepository,
                       PostRepository postRepository,
                       CommentRepository commentRepository,
                       StoryRepository storyRepository,
                       JavaMailSender mailSender) {
        this.flagRepository = flagRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.storyRepository = storyRepository;
        this.mailSender = mailSender;
    }

    @Transactional
    public void flagPost(Long postId, AppUser reporter, String reason) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (flagRepository.existsByReporterAndPost(reporter, post)) {
            throw new RuntimeException("You already flagged this post");
        }

        Flag flag = new Flag();
        flag.setReporter(reporter);
        flag.setPost(post);
        flag.setReason(reason);
        flagRepository.save(flag);

        post.setFlagCount(post.getFlagCount() + 1);

        if (post.getFlagCount() >= FLAG_THRESHOLD) {
            post.setHidden(true);
            sendModerationAlertEmail("Post", postId);
        }

        postRepository.save(post);
    }

    @Transactional
    public void flagComment(Long commentId, AppUser reporter, String reason) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (flagRepository.existsByReporterAndComment(reporter, comment)) {
            throw new RuntimeException("You already flagged this comment");
        }

        Flag flag = new Flag();
        flag.setReporter(reporter);
        flag.setComment(comment);
        flag.setReason(reason);
        flagRepository.save(flag);

        comment.setFlagCount(comment.getFlagCount() + 1);

        if (comment.getFlagCount() >= FLAG_THRESHOLD) {
            comment.setHidden(true);
            sendModerationAlertEmail("Comment", commentId);
        }

        commentRepository.save(comment);
    }

    @Transactional
    public void flagStory(Long storyId, AppUser reporter, String reason) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("Story not found"));

        if (flagRepository.existsByReporterAndStory(reporter, story)) {
            throw new RuntimeException("You already flagged this story");
        }

        Flag flag = new Flag();
        flag.setReporter(reporter);
        flag.setStory(story);
        flag.setReason(reason);
        flagRepository.save(flag);

        story.setFlagCount(story.getFlagCount() + 1);

        if (story.getFlagCount() >= FLAG_THRESHOLD) {
            story.setHidden(true);
            sendModerationAlertEmail("Story", storyId);
        }

        storyRepository.save(story);
    }

    private void sendModerationAlertEmail(String contentType, Long contentId) {
        String to = "trasora.team@gmail.com";
        String subject = "🚨 Content Auto-Hidden for Review (" + contentType + ")";
        String text = "The following " + contentType.toLowerCase() +
                " (ID: " + contentId + ") has been flagged multiple times and was auto-hidden.\n\n" +
                "Please review it in the admin panel.";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}
