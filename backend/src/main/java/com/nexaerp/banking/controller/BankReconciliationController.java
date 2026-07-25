package com.nexaerp.banking.controller;


import com.nexaerp.banking.dto.*;
import com.nexaerp.banking.services.BankReconciliationService;
import com.nexaerp.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/bank-reconciliations")
@RequiredArgsConstructor
public class BankReconciliationController {

    private final BankReconciliationService bankReconciliationService;

    @PostMapping("/start")
    @PreAuthorize("hasAuthority('CREATE_BANKING')")
    public ResponseEntity<ApiResponse<BankReconciliationResponseDto>> start(
            @Valid @RequestBody BankReconciliationStartRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Reconciliation started",
                bankReconciliationService.start(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VIEW_BANKING')")
    public ResponseEntity<ApiResponse<BankReconciliationResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(bankReconciliationService.getById(id)));
    }

    @GetMapping("/account/{bankAccountId}")
    @PreAuthorize("hasAuthority('VIEW_BANKING')")
    public ResponseEntity<ApiResponse<List<BankReconciliationResponseDto>>> getByAccount(
            @PathVariable Long bankAccountId) {
        return ResponseEntity.ok(ApiResponse.success(
                bankReconciliationService.getByBankAccount(bankAccountId)));
    }

    @GetMapping("/{id}/unmatched-transactions")
    @PreAuthorize("hasAuthority('VIEW_BANKING')")
    public ResponseEntity<ApiResponse<List<BankTransactionResponseDto>>> getUnmatched(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                bankReconciliationService.getUnmatchedTransactions(id)));
    }

    @PatchMapping("/{id}/match")
    @PreAuthorize("hasAuthority('EDIT_BANKING')")
    public ResponseEntity<ApiResponse<BankReconciliationResponseDto>> match(
            @PathVariable Long id, @Valid @RequestBody MatchTransactionsRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Transactions matched",
                bankReconciliationService.matchTransactions(id, request.getTransactionIds())));
    }

    @PatchMapping("/{id}/unmatch/{transactionId}")
    @PreAuthorize("hasAuthority('EDIT_BANKING')")
    public ResponseEntity<ApiResponse<BankReconciliationResponseDto>> unmatch(
            @PathVariable Long id, @PathVariable Long transactionId) {
        return ResponseEntity.ok(ApiResponse.success("Transaction unmatched",
                bankReconciliationService.unmatchTransaction(id, transactionId)));
    }

    @PostMapping("/{id}/adjustment")
    @PreAuthorize("hasAuthority('CREATE_BANKING')")
    public ResponseEntity<ApiResponse<BankReconciliationResponseDto>> addAdjustment(
            @PathVariable Long id, @Valid @RequestBody BankTransactionRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Adjustment recorded",
                bankReconciliationService.addAdjustment(id, request)));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('EDIT_BANKING')")
    public ResponseEntity<ApiResponse<BankReconciliationResponseDto>> complete(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Reconciliation completed",
                bankReconciliationService.complete(id)));
    }

    @PostMapping("/{id}/reopen")
    @PreAuthorize("hasAuthority('EDIT_BANKING')")
    public ResponseEntity<ApiResponse<BankReconciliationResponseDto>> reopen(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Reconciliation reopened",
                bankReconciliationService.reopen(id)));
    }

    // ---- CSV statement import ----

    @PostMapping(value = "/{id}/import-statement", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('CREATE_BANKING')")
    public ResponseEntity<ApiResponse<StatementImportResultDto>> importStatement(
            @PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Statement imported",
                bankReconciliationService.importStatement(id, file)));
    }

    @GetMapping("/{id}/statement-lines")
    @PreAuthorize("hasAuthority('VIEW_BANKING')")
    public ResponseEntity<ApiResponse<List<BankStatementLineResponseDto>>> getStatementLines(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                bankReconciliationService.getStatementLines(id)));
    }

    @PatchMapping("/{id}/statement-lines/{lineId}/match")
    @PreAuthorize("hasAuthority('EDIT_BANKING')")
    public ResponseEntity<ApiResponse<BankStatementLineResponseDto>> matchStatementLine(
            @PathVariable Long id, @PathVariable Long lineId,
            @Valid @RequestBody MatchStatementLineRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Statement line matched",
                bankReconciliationService.matchStatementLine(id, lineId, request.getTransactionId())));
    }

    @PostMapping("/{id}/statement-lines/{lineId}/convert-to-adjustment")
    @PreAuthorize("hasAuthority('CREATE_BANKING')")
    public ResponseEntity<ApiResponse<BankStatementLineResponseDto>> convertLineToAdjustment(
            @PathVariable Long id, @PathVariable Long lineId,
            @Valid @RequestBody ConvertLineToAdjustmentRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Adjustment created from statement line",
                bankReconciliationService.convertLineToAdjustment(
                        id, lineId, request.getContraAccountId(), request.getDescription())));
    }
}
