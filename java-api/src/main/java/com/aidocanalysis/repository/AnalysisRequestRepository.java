package com.aidocanalysis.repository;

import com.aidocanalysis.model.AnalysisRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisRequestRepository extends JpaRepository<AnalysisRequest, Long> {
}
