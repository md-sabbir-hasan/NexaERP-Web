package com.nexaerp.dashboard.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryDto {
    private UserSummaryDto users;
    private SecuritySummaryDto security;
    private FinanceSummaryDto finance;
    private BusinessSummaryDto business;
    private SystemSummaryDto system;
    private HealthSummaryDto health;
    private List<RecentActivityDto> recentActivities;
    private BudgetDashboardDto budget;
    private ExpenseDashboardDto expense;
}
