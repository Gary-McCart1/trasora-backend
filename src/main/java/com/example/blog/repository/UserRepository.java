package com.example.blog.repository;

import com.example.blog.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
    Optional<AppUser> findByEmail(String email);
    List<AppUser> findTop5ByUsernameContainingIgnoreCase(String username);
    Optional<AppUser> findByVerificationToken(String token);
    Optional<AppUser> findByPasswordResetToken(String token);
}
