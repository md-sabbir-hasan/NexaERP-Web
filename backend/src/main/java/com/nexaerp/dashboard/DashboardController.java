package com.nexaerp.dashboard;

import com.nexaerp.common.response.ApiResponse;
import com.nexaerp.dashboard.dto.DashboardSummaryDto;
import com.nexaerp.dashboard.dto.DashboardWorkflowSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryDto>> getSummary() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Dashboard summary loaded",
                        dashboardService.getSummary()
                )
        );
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/workflow-summary")
    public ResponseEntity<ApiResponse<DashboardWorkflowSummaryDto>> getWorkflowSummary() {
        return ResponseEntity.ok(ApiResponse.success(
                "Dashboard workflow summary loaded",
                dashboardService.getWorkflowSummary()
        ));
    }
}
