package com.aidocanalysis.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;



import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * S3Service — Handles all Amazon S3 operations.
 *
 * Credentials come from the EC2 IAM Role automatically.
 * No access keys are stored in the code or config files.
 *
 * S3 key structure:
 *   uploads/  → original document files uploaded by users
 *   exports/  → generated analysis report exports
 *   backups/  → SQLite DB backup files
 */
@Service
public class S3Service {

    private final S3Client s3Client;
    private final String bucketName;

    public S3Service(
            @Value("${app.s3.bucket-name}") String bucketName,
            @Value("${cloud.aws.region.static}") String region) {
        this.bucketName = bucketName;
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * Upload a user-submitted document file to S3.
     * Returns the S3 key (use to retrieve later or pass to the user).
     */
    public String uploadDocument(MultipartFile file) throws Exception {
        String key = "uploads/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path temp = Files.createTempFile("upload_", file.getOriginalFilename());
        file.transferTo(temp.toFile());

        s3Client.putObject(
            PutObjectRequest.builder().bucket(bucketName).key(key).build(),
            temp
        );

        Files.deleteIfExists(temp);
        return key;
    }

    /**
     * Upload a generated export (e.g. JSON report) to S3.
     * Returns the S3 key.
     */
    public String uploadExport(String content, String fileName) throws Exception {
        String key = "exports/" + UUID.randomUUID() + "_" + fileName;
        Path temp = Files.createTempFile("export_", fileName);
        Files.writeString(temp, content);

        s3Client.putObject(
            PutObjectRequest.builder().bucket(bucketName).key(key).build(),
            temp
        );

        Files.deleteIfExists(temp);
        return key;
    }

    public String getBucketName() {
        return bucketName;
    }
}
