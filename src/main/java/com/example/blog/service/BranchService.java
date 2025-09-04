package com.example.blog.service;

import com.example.blog.dto.BranchDto;
import com.example.blog.entity.AppUser;
import com.example.blog.entity.Branch;
import com.example.blog.entity.Trunk;
import com.example.blog.repository.BranchRepository;
import com.example.blog.repository.TrunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;
    private final TrunkRepository trunkRepository;
    private final UserService userService;
    private final NotificationService notificationService;

    // Add a new branch to a trunk
    public BranchDto addBranch(Long trunkId, BranchDto dto) {
        final AppUser currentUser = userService.getCurrentUser();

        Trunk trunk = trunkRepository.findById(trunkId)
                .orElseThrow(() -> new NoSuchElementException("Trunk not found with id " + trunkId));

        boolean isOwner = trunk.getOwner().getId().equals(currentUser.getId());

        // Only trunk owner or public trunk can add branches
        if (!isOwner && !trunk.isPublicFlag()) {
            throw new SecurityException("You cannot add branches to this trunk");
        }

        // Create and populate branch
        Branch branch = new Branch();
        branch.setSpotifyTrackId(dto.getTrackId());
        branch.setTitle(dto.getTitle());
        branch.setArtist(dto.getArtist());
        branch.setAlbumArtUrl(dto.getAlbumArtUrl());
        branch.setAddedBy(currentUser);

        // Handle position - auto-calculate if not provided
        branch.setPosition(dto.getPosition() != null ? dto.getPosition() : calculateNextPosition(trunkId));
        branch.setTrunk(trunk);

        Branch savedBranch = branchRepository.save(branch);

        dto.setAddedByUsername(currentUser.getUsername());

        // Send notification to trunk owner if someone else added a branch
        if (!isOwner) {
            notificationService.createBranchNotification(
                    trunk.getOwner(),              // recipient
                    currentUser,                   // sender
                    trunk.getName(),               // trunkName
                    branch.getTitle(),             // songTitle
                    branch.getArtist(),            // songArtist
                    branch.getAlbumArtUrl()        // albumArtUrl
            );
        }

        return mapBranchToDto(savedBranch);
    }

    // Helper method to calculate the next position for a trunk
    private Integer calculateNextPosition(Long trunkId) {
        return branchRepository.findMaxPositionByTrunkId(trunkId)
                .orElse(-1) + 1;
    }

    // Remove a branch
    public void removeBranch(Long branchId) {
        final AppUser currentUser = userService.getCurrentUser();

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new NoSuchElementException("Branch not found with id " + branchId));

        Trunk trunk = branch.getTrunk();
        AppUser trunkOwner = trunk.getOwner();

        // Allow removal if current user is the branch creator OR the trunk owner
        if (!trunkOwner.getId().equals(currentUser.getId())
                && !branch.getAddedBy().getId().equals(currentUser.getId())) {
            throw new SecurityException("You cannot remove this branch");
        }

        branchRepository.delete(branch);
    }

    // Map Branch entity to DTO (metadata is now stored in DB)
    private BranchDto mapBranchToDto(Branch branch) {
        BranchDto dto = new BranchDto();
        dto.setId(branch.getId());
        dto.setTrackId(branch.getSpotifyTrackId());
        dto.setTitle(branch.getTitle());
        dto.setArtist(branch.getArtist());
        dto.setAlbumArtUrl(branch.getAlbumArtUrl());
        dto.setPosition(branch.getPosition());
        if (branch.getAddedBy() != null) {
            dto.setAddedByUsername(branch.getAddedBy().getUsername());
        }
        return dto;
    }

    // List all branches for a trunk
    public List<BranchDto> getBranchesForTrunk(Long trunkId) {
        Trunk trunk = trunkRepository.findById(trunkId)
                .orElseThrow(() -> new NoSuchElementException("Trunk not found with id " + trunkId));

        return trunk.getBranches().stream()
                .map(this::mapBranchToDto)
                .collect(Collectors.toList());
    }
}
