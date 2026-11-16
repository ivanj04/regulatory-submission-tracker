package com.ivanjiang.regtracker.service;

import com.ivanjiang.regtracker.dto.DashboardResponse;
import com.ivanjiang.regtracker.model.enums.SubmissionStatus;
import com.ivanjiang.regtracker.model.enums.SubmissionType;
import com.ivanjiang.regtracker.model.enums.TargetAuthority;
import com.ivanjiang.regtracker.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SubmissionRepository submissionRepository;

    public DashboardResponse getDashboard() {
        Map<String, Long> byStatus = Arrays.stream(SubmissionStatus.values())
                .collect(Collectors.toMap(
                        Enum::name,
                        submissionRepository::countByStatus,
                        (a, b) -> a,
                        LinkedHashMap::new));

        Map<String, Long> byType = Arrays.stream(SubmissionType.values())
                .collect(Collectors.toMap(
                        Enum::name,
                        submissionRepository::countByType,
                        (a, b) -> a,
                        LinkedHashMap::new));

        Map<String, Long> byAuthority = Arrays.stream(TargetAuthority.values())
                .collect(Collectors.toMap(
                        Enum::name,
                        submissionRepository::countByTargetAuthority,
                        (a, b) -> a,
                        LinkedHashMap::new));

        LocalDate cutoff = LocalDate.now().plusDays(30);

        return DashboardResponse.builder()
                .countsByStatus(byStatus)
                .countsByType(byType)
                .countsByAuthority(byAuthority)
                .totalSubmissions(submissionRepository.count())
                .upcomingDeadlines(submissionRepository.findUpcomingDeadlines(cutoff))
                .build();
    }
}
