package com.nexaerp.report;


import com.nexaerp.common.response.ApiResponse;
import com.nexaerp.party.PartyType;
import com.nexaerp.report.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    @GetMapping("/ledger/{accountId}")
    @PreAuthorize("hasAuthority('VIEW_LEDGER')")
    public ResponseEntity<ApiResponse<LedgerResponseDto>> getLedger(
            @PathVariable Long accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getLedger(accountId, fromDate, toDate)));
    }

    @GetMapping("/trial-balance")
    @PreAuthorize("hasAuthority('VIEW_TRIAL_BALANCE')")
    public ResponseEntity<ApiResponse<TrialBalanceResponseDto>> getTrialBalance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {

        // If not provided, default to today
        LocalDate date = asOfDate != null ? asOfDate : LocalDate.now();

        return ResponseEntity.ok(ApiResponse.success(
                reportService.getTrialBalance(date)));
    }


    @GetMapping("/profit-loss")
    @PreAuthorize("hasAuthority('VIEW_REPORT')")
    public ResponseEntity<ApiResponse<ProfitLossResponseDto>> getProfitLoss(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getProfitLoss(fromDate, toDate)));
    }

    @GetMapping("/balance-sheet")
    @PreAuthorize("hasAuthority('VIEW_REPORT')")
    public ResponseEntity<ApiResponse<BalanceSheetResponseDto>> getBalanceSheet(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        LocalDate date = asOfDate != null ? asOfDate : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getBalanceSheet(date)));
    }


    @GetMapping("/party-statement/{partyId}")
    @PreAuthorize("hasAuthority('VIEW_REPORT')")
    public ResponseEntity<ApiResponse<PartyStatementResponseDto>> getPartyStatement(
            @PathVariable Long partyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getPartyStatement(partyId, fromDate, toDate)));
    }

    @GetMapping("/aging")
    @PreAuthorize("hasAuthority('VIEW_REPORT')")
    public ResponseEntity<ApiResponse<AgingResponseDto>> getAgingReport(
            @RequestParam PartyType partyType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        LocalDate date = asOfDate != null ? asOfDate : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getAgingReport(partyType, date)));
    }
}
