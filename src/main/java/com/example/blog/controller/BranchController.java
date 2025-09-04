package com.example.blog.controller;

import com.example.blog.dto.BranchDto;
import com.example.blog.entity.AppUser;
import com.example.blog.repository.UserRepository;
import com.example.blog.service.BranchService;
import com.example.blog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;
    private final UserService userService;
    private final UserRepository userRepository;
    // Add a new branch to a trunk
    @PostMapping("/trunk/{trunkId}")
    public ResponseEntity<BranchDto> addBranch(@PathVariable Long trunkId, @RequestBody BranchDto dto) {
        BranchDto branch = branchService.addBranch(trunkId, dto);
        AppUser user = userRepository.findByUsername(dto.getAddedByUsername()).orElseThrow(() -> new RuntimeException("No user found with the username: " + dto.getAddedByUsername()));
        userService.incrementBranchCount(user.getUsername());
        return ResponseEntity.ok(branch);
    }

    // Remove a branch
    @DeleteMapping("/{branchId}")
    public ResponseEntity<Void> removeBranch(@PathVariable Long branchId) {
        branchService.removeBranch(branchId);
        return ResponseEntity.noContent().build();
    }

    // List all branches in a trunk
    @GetMapping("/trunk/{trunkId}")
    public ResponseEntity<List<BranchDto>> listBranches(@PathVariable Long trunkId) {
        List<BranchDto> branches = branchService.getBranchesForTrunk(trunkId);
        return ResponseEntity.ok(branches);
    }
}
