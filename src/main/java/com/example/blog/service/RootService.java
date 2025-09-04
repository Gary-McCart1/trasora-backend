package com.example.blog.service;

import com.example.blog.dto.RootDto;
import com.example.blog.dto.RootResponseDto;
import com.example.blog.entity.AppUser;
import com.example.blog.entity.Root;
import com.example.blog.repository.RootRepository;
import com.example.blog.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RootService {

    private final RootRepository rootRepository;
    private final UserRepository userRepository;

    /**
     * Get all roots for a user ordered by position (1–10).
     */
    public List<RootResponseDto> getRootsForUser(String username) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        return rootRepository.findByUserIdOrderByPositionAsc(user.getId())
                .stream()
                .map(root -> new RootResponseDto(
                        root.getId(),
                        root.getTrackTitle(),
                        root.getArtistName(),
                        root.getAlbumArtUrl(),
                        root.getTrackId(),
                        root.getPosition(),
                        root.getUser().getUsername()
                ))
                .toList();
    }

    /**
     * Add a new root (track) for the user.
     * If they already have 10, throw exception.
     */
    @Transactional
    public Root addRoot(String username, RootDto dto) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        List<Root> existingRoots = rootRepository.findByUserIdOrderByPositionAsc(user.getId());
        if (existingRoots.size() >= 10) {
            throw new IllegalStateException("User already has 10 roots");
        }

        // Ensure unique track in Roots list
        boolean duplicate = existingRoots.stream()
                .anyMatch(r -> r.getTrackId().equals(dto.getTrackId()));
        if (duplicate) {
            throw new IllegalStateException("Track already in roots");
        }

        // Create a new Root entity from DTO
        Root root = new Root();
        root.setUser(user);
        root.setTrackId(dto.getTrackId());
        root.setTrackTitle(dto.getTitle());
        root.setArtistName(dto.getArtist());
        root.setAlbumArtUrl(dto.getAlbumArtUrl());
        root.setPosition(dto.getPosition());

        return rootRepository.save(root);
    }

    /**
     * Reorder roots — list of root IDs in new order.
     */
    @Transactional
    public void reorderRoots(String username, List<Long> newOrder) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        List<Root> roots = rootRepository.findByUserIdOrderByPositionAsc(user.getId());
        if (roots.size() != newOrder.size()) {
            throw new IllegalArgumentException("Mismatch in roots count");
        }

        for (int i = 0; i < newOrder.size(); i++) {
            Long rootId = newOrder.get(i);
            Root root = roots.stream()
                    .filter(r -> r.getId().equals(rootId))
                    .findFirst()
                    .orElseThrow(() -> new EntityNotFoundException("Root not found"));
            root.setPosition(i + 1);
        }
    }

    /**
     * Remove a root and shift positions up.
     */
    @Transactional
    public void removeRoot(String username, Long rootId) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Root root = rootRepository.findById(rootId)
                .orElseThrow(() -> new EntityNotFoundException("Root not found"));

        if (!root.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("Not your root");
        }

        rootRepository.delete(root);

        // Re-shift remaining positions
        List<Root> remaining = rootRepository.findByUserIdOrderByPositionAsc(user.getId());
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).setPosition(i + 1);
        }
    }
}
