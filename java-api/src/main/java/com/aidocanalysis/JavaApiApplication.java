package com.aidocanalysis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI Document Analysis Service — Java API
 *
 * This is the main backend and orchestrator for the pilot project.
 * It receives requests from the React frontend, calls the Python
 * AI service for document analysis, stores results in SQLite,
 * and handles file operations with Amazon S3.
 */
@SpringBootApplication
public class JavaApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(JavaApiApplication.class, args);
    }
}
