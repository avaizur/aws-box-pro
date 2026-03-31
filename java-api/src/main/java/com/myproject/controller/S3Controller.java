package com.myproject.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;

/**
 * Demonstrates a simple S3 upload from the Java API.
 * POST /api/upload  (multipart file)  → uploads to S3, returns file key.
 */
@RestController
@RequestMapping("/api")
public class S3Controller {

    @Value("${app.s3.bucket-name}")
    private String bucketName;

    @Value("${cloud.aws.region.static}")
    private String awsRegion;

    /**
     * Upload a file to S3.
     * Usage: curl -F "file=@myfile.pdf" http://18.168.203.82/api/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        try {
            String key = "uploads/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

            // Save temp file
            Path temp = Files.createTempFile("upload_", file.getOriginalFilename());
            file.transferTo(temp.toFile());

            // Upload to S3 using default credential chain (IAM role on EC2)
            S3Client s3 = S3Client.builder()
                    .region(Region.of(awsRegion))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();

            s3.putObject(
                PutObjectRequest.builder().bucket(bucketName).key(key).build(),
                temp
            );

            Files.deleteIfExists(temp);

            return ResponseEntity.ok(Map.of(
                "status", "uploaded",
                "bucket", bucketName,
                "key", key
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
