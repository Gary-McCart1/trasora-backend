package com.example.blog.service;

import com.example.blog.entity.AppUser;
import com.example.blog.entity.Follow;
import com.example.blog.entity.NotificationType;
import com.example.blog.repository.FollowRepository;
import com.example.blog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * Handles following a user. Supports private accounts and prevents duplicates.
     */
    public String followUser(String followerUsername, String followingUsername) {
        AppUser follower = getUserByUsername(followerUsername);
        AppUser following = getUserByUsername(followingUsername);

        Follow existingFollow = followRepository.findByFollowerAndFollowing(follower, following)
                .orElse(null);

        if (existingFollow != null) {
            if (existingFollow.isAccepted()) return "following";
            // Pending request exists → cancel first
            notificationService.markFollowRequestAsReadForUser(follower, following);
            followRepository.delete(existingFollow);
        }

        Follow follow = new Follow();
        follow.setFollower(follower);
        follow.setFollowing(following);
        follow.setFollowedAt(Instant.now());

        if (!following.isProfilePublic()) {
            // Private account → pending request
            follow.setAccepted(false);
            followRepository.save(follow);

            notificationService.createNotification(
                    following, follower, NotificationType.FOLLOW_REQUEST,null, follow, null, null, null, null
            );

            return "requested";
        } else {
            // Public account → auto accept
            follow.setAccepted(true);
            followRepository.save(follow);

            notificationService.createNotification(
                    following, follower, NotificationType.FOLLOW,null, null, null, null, null, null
            );

            return "following";
        }
    }

    /**
     * Accepts a pending follow request and marks notification as read.
     */
    public void acceptFollowRequest(Long followId, String currentUsername) {
        AppUser currentUser = getUserByUsername(currentUsername);

        Follow follow = followRepository.findById(followId)
                .orElseThrow(() -> new RuntimeException("Follow request not found"));

        if (!follow.getFollowing().equals(currentUser)) {
            throw new RuntimeException("Cannot accept request not sent to you");
        }

        follow.setAccepted(true);
        followRepository.save(follow);

        notificationService.createNotification(
                follow.getFollower(), currentUser, NotificationType.FOLLOW_ACCEPTED,null, null, null, null, null, null
        );

        // Mark the original follow request notification as read by follow ID
        notificationService.markFollowRequestAsReadForFollowId(followId);
    }

    /**
     * Rejects a pending follow request and marks notification as read.
     */
    @Transactional
    public void rejectFollowRequest(Long followId, String currentUsername) {
        AppUser currentUser = getUserByUsername(currentUsername);

        Follow follow = followRepository.findById(followId)
                .orElseThrow(() -> new RuntimeException("Follow request not found"));

        if (!follow.getFollowing().equals(currentUser)) {
            throw new RuntimeException("Cannot reject request not sent to you");
        }

        // 1️⃣ Mark related notifications as read
        notificationService.markFollowRequestAsReadForFollowId(followId);

        // 2️⃣ Delete related notifications
        notificationService.deleteNotificationsForFollow(follow);

        // 3️⃣ Delete the follow
        followRepository.delete(follow);
    }


    /**
     * Unfollows a user if a follow exists.
     */
    public void unfollowUser(String followerUsername, String followingUsername) {
        AppUser follower = getUserByUsername(followerUsername);
        AppUser following = getUserByUsername(followingUsername);

        followRepository.findByFollowerAndFollowing(follower, following)
                .ifPresent(follow -> {
                    notificationService.deleteNotificationsForFollow(follow);
                    followRepository.delete(follow);
                });
    }

    public boolean isFollowing(String followerUsername, Long followingId) {
        AppUser follower = getUserByUsername(followerUsername);
        AppUser following = getUserById(followingId);

        return followRepository.findByFollowerAndFollowing(follower, following)
                .map(Follow::isAccepted)
                .orElse(false);
    }

    public long getFollowerCount(Long userId) {
        return followRepository.countByFollowing_IdAndAcceptedTrue(userId);
    }

    public long getFollowingCount(Long userId) {
        return followRepository.countByFollowerIdAndAcceptedTrue(userId);
    }

    public List<Follow> getPendingRequests(String username) {
        AppUser user = getUserByUsername(username);
        return followRepository.findAllByFollowingAndAcceptedFalse(user);
    }

    public List<Follow> getSentRequests(String username) {
        AppUser user = getUserByUsername(username);
        return followRepository.findAllByFollowerAndAcceptedFalse(user);
    }

    public String getFollowStatus(String followerUsername, Long followingId) {
        AppUser follower = getUserByUsername(followerUsername);
        AppUser following = getUserById(followingId);

        return followRepository.findByFollowerAndFollowing(follower, following)
                .map(follow -> follow.isAccepted() ? "following" : "requested")
                .orElse("not-following");
    }

    private AppUser getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private AppUser getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public Long getUserIdByUsername(String username) {
        return getUserByUsername(username).getId();
    }


    public List<AppUser> getSuggestedFollows(String username) {
        AppUser currentUser = getUserByUsername(username);

        // 1. Try friends-of-friends suggestions
        List<AppUser> suggestions = followRepository.findSuggestedFollows(currentUser);

        // 2. Get users already followed by current user (map Follow -> AppUser)
        List<AppUser> alreadyFollowing = followRepository
                .findAllByFollowerAndAcceptedTrue(currentUser)
                .stream()
                .map(Follow::getFollowing) // get the followed user
                .toList();

        // 3. Remove already-followed users from suggestions
        suggestions.removeAll(alreadyFollowing);

        // 4. Ensure at least 3 suggestions using top-followed users
        if (suggestions.size() < 3) {
            List<AppUser> topFollowed = followRepository.findTopFollowedUsers(PageRequest.of(0, 10));

            // Exclude current user, already suggested, and already following
            topFollowed.remove(currentUser);
            topFollowed.removeAll(suggestions);
            topFollowed.removeAll(alreadyFollowing);

            for (AppUser user : topFollowed) {
                if (suggestions.size() >= 3) break;
                suggestions.add(user);
            }
        }

        return suggestions;
    }



}
