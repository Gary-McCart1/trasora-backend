package com.example.blog.repository;

import com.example.blog.entity.Trunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TrunkRepository extends JpaRepository<Trunk, Long> {

    // Find all trunks for a specific owner
    List<Trunk> findByOwnerIdOrderByUpdatedAtDesc(Long ownerId);

    // Find all public trunks
    List<Trunk> findByPublicFlagTrueOrderByUpdatedAtDesc();

    // Find trunks by owner's username and fetch branches
    @Query("SELECT t FROM Trunk t LEFT JOIN FETCH t.branches b WHERE t.owner.username = :username")
    List<Trunk> findByOwnerUsernameWithBranches(@Param("username") String username);

    // Get user's own trunks with branches
    @Query("SELECT t FROM Trunk t LEFT JOIN FETCH t.branches WHERE t.owner.id = :userId")
    List<Trunk> findByOwnerIdWithBranches(@Param("userId") Long userId);

    // Find public trunks from users that the given user follows
    @Query("""
        SELECT t FROM Trunk t
        JOIN com.example.blog.entity.Follow f ON f.following = t.owner
        WHERE f.follower.id = :userId AND t.publicFlag = true
    """)
    List<Trunk> findPublicTrunksByFollowedUsers(@Param("userId") Long userId);
}
