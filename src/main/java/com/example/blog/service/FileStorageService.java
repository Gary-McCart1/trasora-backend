package com.example.blog.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileStorageService {

    private final String uploadDir = "uploads/profile-pictures"; // You can change this location

    public String saveProfilePicture(MultipartFile file) {
        try {
            // Create directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String filename = System.currentTimeMillis() + fileExtension;

            // Save file
            Path filePath = uploadPath.resolve(filename);
            file.transferTo(filePath.toFile());

            // Return relative path or full URL if needed
            return "/uploads/profile-pictures/" + filename;

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }
}
