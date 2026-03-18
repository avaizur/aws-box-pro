package com.aidocanalysis.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Stores the AI analysis output for a given request.
 * Maps to the 'analysis_results' table in SQLite.
 * Has a many-to-one relationship with AnalysisRequest.
 */
@Entity
@Table(name = "analysis_results")
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Links back to the source request. */
    @Column(name = "request_id", nullable = false)
    private Long requestId;

    /** AI-generated summary of the document/text. */
    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    /** Total word count of the input text. */
    @Column(name = "word_count")
    private Integer wordCount;

    /**
     * Simple document classification label returned by the Python AI service.
     * e.g. "technical", "legal", "financial", "general"
     */
    @Column(name = "classification")
    private String classification;

    /** How long the Python AI service took to process (milliseconds). */
    @Column(name = "processing_ms")
    private Long processingMs;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // ── Getters & Setters ────────────────────────────────────

    public Long getId() { return id; }
    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public Integer getWordCount() { return wordCount; }
    public void setWordCount(Integer wordCount) { this.wordCount = wordCount; }
    public String getClassification() { return classification; }
    public void setClassification(String classification) { this.classification = classification; }
    public Long getProcessingMs() { return processingMs; }
    public void setProcessingMs(Long processingMs) { this.processingMs = processingMs; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
