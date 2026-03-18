package com.aidocanalysis.repository;

import com.aidocanalysis.model.AnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {
    List<AnalysisResult> findByRequestId(Long requestId);
}
