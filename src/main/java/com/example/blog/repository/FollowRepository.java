package com.example.blog.repository;

import com.example.blog.entity.AppUser;
import com.example.blog.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    // Check if a follow relation already exists (pending or accepted)
    boolean existsByFollowerAndFollowing(AppUser follower, AppUser following);

    // Find a specific follow relation
    Optional<Follow> findByFollowerAndFollowing(AppUser follower, AppUser following);

    // All followers (pending + accepted)
    List<Follow> findAllByFollowing(AppUser user);

    // All following (pending + accepted)
    List<Follow> findAllByFollower(AppUser user);

    // Count followers (only accepted)
    int countByFollowing_IdAndAcceptedTrue(Long followeeId);

    // Count following (only accepted)
    int countByFollowerIdAndAcceptedTrue(Long followerId);

    // Get accepted followers
    List<Follow> findAllByFollowingAndAcceptedTrue(AppUser user);

    // Get accepted following
    List<Follow> findAllByFollowerAndAcceptedTrue(AppUser user);

    // Get pending follow requests for a user (people who want to follow them)
    List<Follow> findAllByFollowingAndAcceptedFalse(AppUser user);

    // Get pending follow requests sent by a user
    List<Follow> findAllByFollowerAndAcceptedFalse(AppUser user);

    // Remove a follow relationship (accepted or not)
    void deleteByFollowerAndFollowing(AppUser follower, AppUser following);
}
