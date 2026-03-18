package com.aidocanalysis.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a document/text submission by the user.
 * Maps to the 'analysis_requests' table in SQLite.
 */
@Entity
@Table(name = "analysis_requests")
public class AnalysisRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Raw text submitted by the user, or extracted from an uploaded file. */
    @Column(name = "input_text", columnDefinition = "TEXT")
    private String inputText;

    /** Original filename if a document was uploaded. Null for plain text input. */
    @Column(name = "file_name")
    private String fileName;

    /** S3 key where the uploaded file is stored. Null for plain text input. */
    @Column(name = "s3_key")
    private String s3Key;

    /** Processing status: 'pending', 'completed', 'failed'. */
    @Column(name = "status")
    private String status = "pending";

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // ── Getters & Setters ────────────────────────────────────

    public Long getId() { return id; }
    public String getInputText() { return inputText; }
    public void setInputText(String inputText) { this.inputText = inputText; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getS3Key() { return s3Key; }
    public void setS3Key(String s3Key) { this.s3Key = s3Key; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
