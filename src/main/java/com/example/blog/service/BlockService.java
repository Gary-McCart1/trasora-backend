package com.example.blog.service;

import com.example.blog.entity.AppUser;
import com.example.blog.entity.Block;
import com.example.blog.repository.BlockRepository;
import com.example.blog.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BlockService {

    private final BlockRepository blockRepository;
    private final UserRepository userRepository;

    public BlockService(BlockRepository blockRepository, UserRepository userRepository) {
        this.blockRepository = blockRepository;
        this.userRepository = userRepository;
    }

    public void blockUser(AppUser blocker, Long blockedUserId) {
        AppUser blocked = userRepository.findById(blockedUserId)
                .orElseThrow(() -> new RuntimeException("User to block not found"));

        if (blockRepository.findByBlockerAndBlocked(blocker, blocked).isEmpty()) {
            Block block = Block.builder().blocker(blocker).blocked(blocked).build();
            blockRepository.save(block);
        }
    }

    public void unblockUser(AppUser blocker, Long blockedUserId) {
        AppUser blocked = userRepository.findById(blockedUserId)
                .orElseThrow(() -> new RuntimeException("User to unblock not found"));

        blockRepository.findByBlockerAndBlocked(blocker, blocked)
                .ifPresent(blockRepository::delete);
    }

    public List<Block> getBlockedUsers(AppUser blocker) {
        return blockRepository.findAllByBlocker(blocker);
    }

    public boolean isBlocked(AppUser viewer, AppUser contentOwner) {
        return blockRepository.findByBlockerAndBlocked(contentOwner, viewer).isPresent();
    }
}
