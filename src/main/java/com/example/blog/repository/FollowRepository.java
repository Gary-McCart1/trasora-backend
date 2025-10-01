package com.example.blog.repository;

import com.example.blog.entity.AppUser;
import com.example.blog.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // Suggested follows: people followed by your followees
    @Query("""
        SELECT DISTINCT f2.following
        FROM Follow f1
        JOIN Follow f2 ON f1.following = f2.follower
        WHERE f1.follower = :user AND f2.following <> :user AND f2.accepted = true
    """)
    List<AppUser> findSuggestedFollows(@Param("user") AppUser user);

    // Top N most-followed users
    @Query("""
        SELECT u
        FROM AppUser u
        LEFT JOIN Follow f ON f.following = u AND f.accepted = true
        GROUP BY u
        ORDER BY COUNT(f.id) DESC
    """)
    List<AppUser> findTopFollowedUsers(org.springframework.data.domain.Pageable pageable);
}
