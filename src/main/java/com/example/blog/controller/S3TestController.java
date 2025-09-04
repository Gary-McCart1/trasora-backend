package com.example.blog.controller;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
public class S3TestController {

    private final S3Client s3Client;

    public S3TestController(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @GetMapping("/test/s3")
    public String testS3Connection() {
        ListBucketsResponse buckets = s3Client.listBuckets();
        return "Buckets: " + buckets.buckets().stream()
                .map(b -> b.name())
                .collect(Collectors.joining(", "));
    }

}

