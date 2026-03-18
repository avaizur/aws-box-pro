package com.aidocanalysis.controller;

import com.aidocanalysis.model.AnalysisRequest;
import com.aidocanalysis.model.AnalysisResult;
import com.aidocanalysis.repository.AnalysisRequestRepository;
import com.aidocanalysis.repository.AnalysisResultRepository;
import com.aidocanalysis.service.AiServiceClient;
import com.aidocanalysis.service.S3Service;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * AnalysisController — Main API controller.
 *
 * Endpoints:
 *   POST /api/analyze/text    → Submit plain text for analysis
 *   POST /api/analyze/file    → Upload a document file for analysis
 *   GET  /api/analyze/history → Get list of past analysis requests
 *   GET  /api/analyze/{id}    → Get result for a specific request
 *   POST /api/analyze/{id}/export → Export result as JSON to S3
 *   GET  /api/health          → Service health check
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AnalysisController {

    private final AnalysisRequestRepository requestRepo;
    private final AnalysisResultRepository resultRepo;
    private final AiServiceClient aiClient;
    private final S3Service s3Service;

    public AnalysisController(
            AnalysisRequestRepository requestRepo,
            AnalysisResultRepository resultRepo,
            AiServiceClient aiClient,
            S3Service s3Service) {
        this.requestRepo = requestRepo;
        this.resultRepo = resultRepo;
        this.aiClient = aiClient;
        this.s3Service = s3Service;
    }

    // ── Health Check ──────────────────────────────────────────────────────────

    /** GET /api/health */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "ok",
            "service", "java-api",
            "version", "1.0.0"
        ));
    }

    // ── Text Analysis ─────────────────────────────────────────────────────────

    /**
     * POST /api/analyze/text
     * Body: { "text": "Your document content here..." }
     *
     * Flow:
     * 1. Save the request to SQLite (status = pending)
     * 2. Call Python AI service with the text
     * 3. Save the AI result to SQLite (status = completed)
     * 4. Return the full result to the frontend
     */
    @PostMapping("/analyze/text")
    public ResponseEntity<?> analyzeText(@RequestBody Map<String, String> body) {
        String text = body.get("text");
        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Field 'text' is required."));
        }

        return runAnalysis(text, null, null);
    }

    /**
     * POST /api/analyze/file
     * Form-data: file=<document>
     *
     * Flow:
     * 1. Upload file to S3 (uploads/ prefix)
     * 2. Extract text from the file content
     * 3. Run the same analysis pipeline as text input
     * 4. Return result including S3 key
     */
    @PostMapping("/analyze/file")
    public ResponseEntity<?> analyzeFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Uploaded file is empty."));
        }

        try {
            // Extract text from file (basic UTF-8 text extraction)
            String text = new String(file.getBytes(), StandardCharsets.UTF_8);

            // Upload original file to S3
            String s3Key = s3Service.uploadDocument(file);

            return runAnalysis(text, file.getOriginalFilename(), s3Key);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "File processing failed: " + e.getMessage()));
        }
    }

    // ── History & Retrieval ───────────────────────────────────────────────────

    /** GET /api/analyze/history — Returns recent analysis requests */
    @GetMapping("/analyze/history")
    public ResponseEntity<List<Map<String, Object>>> history() {
        List<Map<String, Object>> history = new ArrayList<>();
        for (AnalysisRequest req : requestRepo.findAll()) {
            List<AnalysisResult> results = resultRepo.findByRequestId(req.getId());
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("requestId", req.getId());
            entry.put("fileName", req.getFileName());
            entry.put("status", req.getStatus());
            entry.put("createdAt", req.getCreatedAt());
            if (!results.isEmpty()) {
                AnalysisResult r = results.get(0);
                entry.put("summary", r.getSummary());
                entry.put("wordCount", r.getWordCount());
                entry.put("classification", r.getClassification());
            }
            history.add(entry);
        }
        return ResponseEntity.ok(history);
    }

    /** GET /api/analyze/{id} — Returns result for a specific request */
    @GetMapping("/analyze/{id}")
    public ResponseEntity<?> getResult(@PathVariable Long id) {
        Optional<AnalysisRequest> req = requestRepo.findById(id);
        if (req.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<AnalysisResult> results = resultRepo.findByRequestId(id);
        if (results.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(buildResponse(req.get(), results.get(0)));
    }

    /**
     * POST /api/analyze/{id}/export
     * Exports the analysis result to S3 as a JSON file.
     * Returns the S3 key.
     */
    @PostMapping("/analyze/{id}/export")
    public ResponseEntity<?> exportResult(@PathVariable Long id) {
        Optional<AnalysisRequest> req = requestRepo.findById(id);
        if (req.isEmpty()) return ResponseEntity.notFound().build();

        List<AnalysisResult> results = resultRepo.findByRequestId(id);
        if (results.isEmpty()) return ResponseEntity.notFound().build();

        try {
            Map<String, Object> export = buildResponse(req.get(), results.get(0));
            String json = export.toString(); // Simple — swap for Jackson ObjectMapper if needed
            String s3Key = s3Service.uploadExport(json, "analysis-result-" + id + ".json");

            return ResponseEntity.ok(Map.of(
                "status", "exported",
                "s3Key", s3Key,
                "bucket", s3Service.getBucketName()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Export failed: " + e.getMessage()));
        }
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private ResponseEntity<?> runAnalysis(String text, String fileName, String s3Key) {
        // 1. Save request
        AnalysisRequest req = new AnalysisRequest();
        req.setInputText(text);
        req.setFileName(fileName);
        req.setS3Key(s3Key);
        req.setStatus("pending");
        req = requestRepo.save(req);

        try {
            // 2. Call Python AI service
            Map<String, Object> aiResponse = aiClient.analyze(text);

            // 3. Save result
            AnalysisResult result = new AnalysisResult();
            result.setRequestId(req.getId());
            result.setSummary((String) aiResponse.get("summary"));
            result.setWordCount(((Number) aiResponse.get("word_count")).intValue());
            result.setClassification((String) aiResponse.get("classification"));
            result.setProcessingMs(((Number) aiResponse.get("processing_ms")).longValue());
            resultRepo.save(result);

            // 4. Update request status
            req.setStatus("completed");
            requestRepo.save(req);

            return ResponseEntity.ok(buildResponse(req, result));

        } catch (Exception e) {
            req.setStatus("failed");
            requestRepo.save(req);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Analysis failed: " + e.getMessage()));
        }
    }

    private Map<String, Object> buildResponse(AnalysisRequest req, AnalysisResult result) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("requestId", req.getId());
        resp.put("status", req.getStatus());
        resp.put("fileName", req.getFileName());
        resp.put("s3Key", req.getS3Key());
        resp.put("summary", result.getSummary());
        resp.put("wordCount", result.getWordCount());
        resp.put("classification", result.getClassification());
        resp.put("processingMs", result.getProcessingMs());
        resp.put("createdAt", req.getCreatedAt());
        return resp;
    }
}
