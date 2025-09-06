package com.example.blog.controller;

import com.example.blog.dto.RootDto;
import com.example.blog.dto.RootResponseDto;
import com.example.blog.entity.Root;
import com.example.blog.service.RootService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/roots")
@RequiredArgsConstructor
public class RootController {

    private final RootService rootService;

    /** Get current user's roots in order */
    @GetMapping("/{username}")
    public ResponseEntity<List<RootResponseDto>> getMyRoots(@PathVariable String username) {
        List<RootResponseDto> roots = rootService.getRootsForUser(username);
        return ResponseEntity.ok(roots);
    }

    /** Add a new root for the current user */
    @PostMapping
    public ResponseEntity<RootResponseDto> addRoot(@RequestBody RootDto dto, Principal principal) {
        String username = principal.getName();
        Root saved = rootService.addRoot(username, dto);

        RootResponseDto responseDto = new RootResponseDto(
                saved.getId(),
                saved.getTrackTitle(),
                saved.getArtistName(),
                saved.getAlbumArtUrl(),
                saved.getTrackId(),
                saved.getPosition(),
                saved.getUser().getUsername()
        );

        return ResponseEntity.ok(responseDto);
    }

    /** Reorder roots for current user */
    @PostMapping("/reorder")
    public ResponseEntity<Void> reorderRoots(@RequestBody List<Long> newOrder, Principal principal) {
        String username = principal.getName();
        rootService.reorderRoots(username, newOrder);
        return ResponseEntity.noContent().build();
    }

    /** Remove a root from the list */
    @DeleteMapping("/{rootId}")
    public ResponseEntity<Void> deleteRoot(@PathVariable Long rootId, Principal principal) {
        String username = principal.getName();
        rootService.removeRoot(username, rootId);
        return ResponseEntity.noContent().build();
    }
}
