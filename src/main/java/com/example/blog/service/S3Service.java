package com.example.blog.service;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;

import java.io.InputStream;

@Service
public class S3Service {

    private final S3Client s3Client;
    private final String bucketName = "dreamr-user-content"; // your bucket

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public String uploadFile(String key, InputStream inputStream, long contentLength, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentLength(contentLength)
                .contentType(contentType)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, contentLength));
        return key;
    }

    public void deleteFile(String key) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        s3Client.deleteObject(deleteObjectRequest);
    }

    public String getBucketName() {
        return bucketName;
    }
}
