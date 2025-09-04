package com.example.blog.controller;

import com.example.blog.service.S3Service;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/test")
public class TestS3UploadController {

    private final S3Service s3Service;

    public TestS3UploadController(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadTestFile(@RequestParam("file") MultipartFile file) {
        try {
            String key = "test-uploads/" + file.getOriginalFilename();
            String uploadedKey = s3Service.uploadFile(key, file.getInputStream(), file.getSize(), file.getContentType());
            return ResponseEntity.ok("Uploaded file key: " + uploadedKey);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Upload failed: " + e.getMessage());
        }
    }
}
