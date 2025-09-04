package com.example.blog.controller;

import com.example.blog.dto.TrunkDto;
import com.example.blog.entity.AppUser;
import com.example.blog.service.TrunkService;
import com.example.blog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/trunks")
@RequiredArgsConstructor
public class TrunkController {

    private final TrunkService trunkService;
    private final UserService userService;

    // --- Available trunks endpoint first (specific path) ---
    @GetMapping("/available/trunks")
    public ResponseEntity<List<TrunkDto>> getAvailableTrunks(@AuthenticationPrincipal UserDetails userDetails) {
        AppUser currentUser = userService.getCurrentUser();
        List<TrunkDto> trunks = trunkService.getAvailableTrunksForUser(currentUser.getId());
        return ResponseEntity.ok(trunks);
    }

    // --- Create a new trunk ---
    @PostMapping
    public ResponseEntity<TrunkDto> createTrunk(@RequestBody TrunkDto dto) {
        TrunkDto createdTrunk = trunkService.createTrunk(dto);
        return ResponseEntity.ok(createdTrunk);
    }

    // --- Get a trunk by numeric ID only ---
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<TrunkDto> getTrunk(@PathVariable Long id) {
        try {
            TrunkDto trunk = trunkService.getTrunk(id);
            return ResponseEntity.ok(trunk);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // --- Delete a trunk by numeric ID ---
    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> deleteTrunk(@PathVariable Long id) {
        try {
            trunkService.deleteTrunk(id);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // --- Update a trunk by numeric ID ---
    @PutMapping("/{id:\\d+}")
    public ResponseEntity<TrunkDto> updateTrunk(@PathVariable Long id, @RequestBody TrunkDto dto) {
        try {
            TrunkDto updated = trunkService.updateTrunk(id, dto);
            return ResponseEntity.ok(updated);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // --- Get all trunks by user ---
    @GetMapping("/user/{username}")
    public ResponseEntity<List<TrunkDto>> getTrunksByUser(@PathVariable String username) {
        try {
            List<TrunkDto> trunks = trunkService.getTrunksByUser(username);
            return ResponseEntity.ok(trunks);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // --- Get all trunks by user with branches ---
    @GetMapping("/user/{username}/with-branches")
    public ResponseEntity<List<TrunkDto>> getTrunksByUserWithBranches(@PathVariable String username) {
        try {
            List<TrunkDto> trunks = trunkService.getTrunksByUser(username);
            return ResponseEntity.ok(trunks);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{trunkId}/title")
    public ResponseEntity<TrunkDto> updateTrunkTitle(
            @PathVariable Long trunkId,
            @RequestBody Map<String, String> requestBody) {

        String newTitle = requestBody.get("title");
        if (newTitle == null || newTitle.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        TrunkDto updatedTrunk = trunkService.updateTrunkTitle(trunkId, newTitle);
        return ResponseEntity.ok(updatedTrunk);
    }

    @PatchMapping("/{trunkId}/visibility")
    public ResponseEntity<TrunkDto> updateTrunkVisibility(
            @PathVariable Long trunkId,
            @RequestBody Map<String, Boolean> requestBody) {

        Boolean publicFlag = requestBody.get("publicFlag");
        if (publicFlag == null) {
            return ResponseEntity.badRequest().build();
        }

        TrunkDto updatedTrunk = trunkService.updateTrunkVisibility(trunkId, publicFlag);
        return ResponseEntity.ok(updatedTrunk);
    }
}
