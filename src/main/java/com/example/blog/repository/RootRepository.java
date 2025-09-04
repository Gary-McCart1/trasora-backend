package com.example.blog.repository;

import com.example.blog.entity.Root;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RootRepository extends JpaRepository<Root, Long> {
    List<Root> findByUserIdOrderByPositionAsc(Long userId);
}
