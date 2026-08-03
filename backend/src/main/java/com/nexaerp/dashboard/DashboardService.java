package com.nexaerp.dashboard;

import com.nexaerp.dashboard.dto.DashboardSummaryDto;
import com.nexaerp.dashboard.dto.DashboardWorkflowSummaryDto;

public interface DashboardService {
    DashboardSummaryDto getSummary();
    DashboardWorkflowSummaryDto getWorkflowSummary();
}
