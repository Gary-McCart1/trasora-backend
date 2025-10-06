package com.example.blog.repository;

import com.example.blog.entity.Block;
import com.example.blog.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BlockRepository extends JpaRepository<Block, Long> {
    List<Block> findAllByBlocker(AppUser blocker);
    Optional<Block> findByBlockerAndBlocked(AppUser blocker, AppUser blocked);
}
