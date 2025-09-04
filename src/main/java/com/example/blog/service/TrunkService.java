package com.example.blog.service;

import com.example.blog.dto.BranchDto;
import com.example.blog.dto.SpotifyTrack;
import com.example.blog.dto.TrunkDto;
import com.example.blog.entity.AppUser;
import com.example.blog.entity.Branch;
import com.example.blog.entity.Trunk;
import com.example.blog.repository.TrunkRepository;
import com.example.blog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class TrunkService {

    private final TrunkRepository trunkRepository;
    private final UserService userService;
    private final UserRepository userRepository; // Added to get user by username
    private final SpotifyService spotifyService;

    // Create a new trunk with branches
    public TrunkDto createTrunk(TrunkDto dto) {
        final Trunk trunk = new Trunk();
        trunk.setName(dto.getName());
        trunk.setDescription(dto.getDescription());
        trunk.setPublicFlag(dto.isPublicFlag());

        final AppUser currentUser = userService.getCurrentUser();
        trunk.setOwner(currentUser);

        if (dto.getBranches() != null && !dto.getBranches().isEmpty()) {
            List<Branch> branches = dto.getBranches().stream().map(branchDto -> {
                Branch b = new Branch();
                b.setSpotifyTrackId(branchDto.getTrackId());
                b.setPosition(branchDto.getPosition());
                b.setTrunk(trunk);
                b.setAddedBy(currentUser);
                return b;
            }).collect(Collectors.toList());
            trunk.setBranches(branches);
        }

        Trunk savedTrunk = trunkRepository.save(trunk);
        return mapToDto(savedTrunk);
    }

    // Get a trunk by ID
    public TrunkDto getTrunk(Long id) {
        Trunk trunk = trunkRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Trunk not found with id " + id));
        return mapToDto(trunk);
    }

    // Map Trunk entity to DTO
    private TrunkDto mapToDto(Trunk trunk) {
        TrunkDto dto = new TrunkDto();
        dto.setId(trunk.getId());
        dto.setName(trunk.getName());
        dto.setDescription(trunk.getDescription());
        dto.setPublicFlag(trunk.isPublicFlag());

        if (trunk.getOwner() != null) {
            dto.setUsername(trunk.getOwner().getUsername());
        }

        if (trunk.getBranches() != null && !trunk.getBranches().isEmpty()) {
            dto.setBranches(trunk.getBranches().stream()
                    .map(this::mapBranchToDto)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    public void deleteTrunk(Long trunkId) {
        final AppUser currentUser = userService.getCurrentUser();
        Trunk trunk = trunkRepository.findById(trunkId)
                .orElseThrow(() -> new NoSuchElementException("Trunk not found with id " + trunkId));

        if (!trunk.getOwner().getId().equals(currentUser.getId())) {
            throw new SecurityException("You are not allowed to delete this trunk");
        }

        trunkRepository.delete(trunk);
    }

    public TrunkDto updateTrunk(Long trunkId, TrunkDto dto) {
        final AppUser currentUser = userService.getCurrentUser();

        Trunk trunk = trunkRepository.findById(trunkId)
                .orElseThrow(() -> new NoSuchElementException("Trunk not found with id " + trunkId));

        if (!trunk.getOwner().getId().equals(currentUser.getId())) {
            throw new SecurityException("You are not allowed to update this trunk");
        }

        trunk.setName(dto.getName());
        trunk.setDescription(dto.getDescription());
        trunk.setPublicFlag(dto.isPublicFlag());

        if (dto.getBranches() != null) {
            trunk.getBranches().clear();

            List<Branch> newBranches = dto.getBranches().stream().map(branchDto -> {
                Branch b = new Branch();
                b.setSpotifyTrackId(branchDto.getTrackId());
                b.setPosition(branchDto.getPosition());
                b.setTrunk(trunk);
                b.setAddedBy(currentUser);
                b.setArtist(branchDto.getArtist());
                b.setTitle(branchDto.getTitle());
                return b;
            }).collect(Collectors.toList());

            trunk.setBranches(newBranches);
        }

        Trunk updatedTrunk = trunkRepository.save(trunk);
        return mapToDto(updatedTrunk);
    }

    // Map Branch entity to DTO with Spotify metadata
    private BranchDto mapBranchToDto(Branch branch) {
        BranchDto dto = new BranchDto();
        dto.setTrackId(branch.getSpotifyTrackId());
        dto.setPosition(branch.getPosition());

        try {
            // Fetch track metadata from Spotify
            SpotifyTrack track = spotifyService.getTrack(branch.getSpotifyTrackId());
            dto.setTitle(track.getName());
            dto.setArtist(track.getArtistNames()); // combine multiple artists if needed
            dto.setAlbumArtUrl(track.getAlbumImageUrl());
        } catch (Exception e) {
            // Fallback for when Spotify API call fails
            System.err.println("Failed to fetch Spotify track metadata for trackId: " + branch.getSpotifyTrackId());
            dto.setTitle("Unknown Title");
            dto.setArtist("Unknown Artist");
            dto.setAlbumArtUrl("https://via.placeholder.com/150"); // Placeholder image
        }

        return dto;
    }

    public List<TrunkDto> getTrunksByUser(String username) {
        List<Trunk> trunks = trunkRepository.findByOwnerUsernameWithBranches(username);
        return trunks.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<TrunkDto> getAvailableTrunksForUser(Long userId) {
        // 1. Get user’s own trunks
        List<Trunk> ownTrunks = trunkRepository.findByOwnerIdWithBranches(userId);

        // 2. Get public trunks of users they follow
        List<Trunk> followedPublicTrunks = trunkRepository.findPublicTrunksByFollowedUsers(userId);

        // 3. Combine
        List<Trunk> combined = Stream.concat(ownTrunks.stream(), followedPublicTrunks.stream())
                .collect(Collectors.toList());

        // 4. Map to DTO
        return combined.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public TrunkDto updateTrunkTitle(Long trunkId, String title) {
        final AppUser currentUser = userService.getCurrentUser();

        Trunk trunk = trunkRepository.findById(trunkId)
                .orElseThrow(() -> new NoSuchElementException("Trunk not found with id " + trunkId));

        if (!trunk.getOwner().getId().equals(currentUser.getId())) {
            throw new SecurityException("You are not allowed to update this trunk");
        }

        trunk.setName(title);
        return mapToDto(trunkRepository.save(trunk));
    }

    public TrunkDto updateTrunkVisibility(Long trunkId, boolean publicFlag){
        final AppUser currentUser = userService.getCurrentUser();
        Trunk trunk = trunkRepository.findById(trunkId).orElseThrow(() -> new NoSuchElementException("Trunk not found with id " + trunkId));
        if (!trunk.getOwner().getId().equals(currentUser.getId())) {
            throw new SecurityException("You are not allowed to update this trunk");
        }
        trunk.setPublicFlag(publicFlag);
        return mapToDto(trunkRepository.save(trunk));
    }


}