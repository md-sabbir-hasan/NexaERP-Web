package com.nexaerp.excel;

import com.nexaerp.party.PartyType;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ExcelReportController {

    private final ExcelReportService excelReportService;

    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    @GetMapping("/ledger/excel")
    @PreAuthorize("hasAuthority('VIEW_LEDGER')")
    public ResponseEntity<byte[]> exportLedger(
            @RequestParam Long accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        byte[] bytes = excelReportService.generateLedgerExcel(accountId, fromDate, toDate);
        return download(bytes, "ledger-report.xlsx");
    }

    @GetMapping("/trial-balance/excel")
    @PreAuthorize("hasAuthority('VIEW_TRIAL_BALANCE')")
    public ResponseEntity<byte[]> exportTrialBalance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {

        byte[] bytes = excelReportService.generateTrialBalanceExcel(asOfDate);
        return download(bytes, "trial-balance.xlsx");
    }

    @GetMapping("/profit-loss/excel")
    @PreAuthorize("hasAuthority('VIEW_REPORT')")
    public ResponseEntity<byte[]> exportProfitLoss(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        byte[] bytes = excelReportService.generateProfitLossExcel(fromDate, toDate);
        return download(bytes, "profit-and-loss.xlsx");
    }

    @GetMapping("/balance-sheet/excel")
    @PreAuthorize("hasAuthority('VIEW_REPORT')")
    public ResponseEntity<byte[]> exportBalanceSheet(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {

        byte[] bytes = excelReportService.generateBalanceSheetExcel(asOfDate);
        return download(bytes, "balance-sheet.xlsx");
    }

    @GetMapping("/party-statement/excel")
    @PreAuthorize("hasAuthority('VIEW_REPORT')")
    public ResponseEntity<byte[]> exportPartyStatement(
            @RequestParam Long partyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        byte[] bytes = excelReportService.generatePartyStatementExcel(partyId, fromDate, toDate);
        return download(bytes, "party-statement.xlsx");
    }

    @GetMapping("/aging/excel")
    @PreAuthorize("hasAuthority('VIEW_REPORT')")
    public ResponseEntity<byte[]> exportAging(
            @RequestParam PartyType partyType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {

        byte[] bytes = excelReportService.generateAgingExcel(partyType, asOfDate);
        return download(bytes, "aging-report.xlsx");
    }

    private ResponseEntity<byte[]> download(byte[] bytes, String filename) {
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(bytes);
    }
}