package com.nexaerp.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardWorkflowSummaryDto {
    private boolean approvalEnabled;
    private Long availablePendingCount;
    private LocalDateTime oldestAvailableSubmittedAt;
    private Long myPendingCount;
    private Long myReturnedCount;
    private Long myApprovedUnconsumedCount;
}
