package com.example.blog.repository;

import com.example.blog.entity.Branch;
import com.example.blog.entity.Trunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    List<Branch> findByTrunkIdOrderByPositionAscIdAsc(Long trunkId);
    boolean existsByTrunkIdAndSpotifyTrackId(Long trunkId, String spotifyTrackId);
    long countByTrunkId(Long trunkId);
    @Query("SELECT MAX(b.position) FROM Branch b WHERE b.trunk.id = :trunkId")
    Optional<Integer> findMaxPositionByTrunkId(@Param("trunkId") Long trunkId);
}
