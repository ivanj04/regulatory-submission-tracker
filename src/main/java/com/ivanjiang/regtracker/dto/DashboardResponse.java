package com.ivanjiang.regtracker.dto;

import com.ivanjiang.regtracker.model.Submission;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class DashboardResponse {
    private Map<String, Long> countsByStatus;
    private Map<String, Long> countsByType;
    private Map<String, Long> countsByAuthority;
    private long totalSubmissions;
    private List<Submission> upcomingDeadlines;
}
