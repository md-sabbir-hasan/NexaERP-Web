package com.nexaerp.banking.controller;


import com.nexaerp.banking.dto.BankTransactionRequestDto;
import com.nexaerp.banking.dto.BankTransactionResponseDto;
import com.nexaerp.banking.dto.BankTransferRequestDto;
import com.nexaerp.banking.dto.BankTransferResponseDto;
import com.nexaerp.banking.services.BankTransactionService;
import com.nexaerp.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/bank-transactions")
@RequiredArgsConstructor
public class BankTransactionController {

    private final BankTransactionService bankTransactionService;

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_BANKING')")
    public ResponseEntity<ApiResponse<List<BankTransactionResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(bankTransactionService.getAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VIEW_BANKING')")
    public ResponseEntity<ApiResponse<BankTransactionResponseDto>> getById(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(bankTransactionService.getById(id)));
    }

    @GetMapping("/account/{bankAccountId}")
    @PreAuthorize("hasAuthority('VIEW_BANKING')")
    public ResponseEntity<ApiResponse<List<BankTransactionResponseDto>>> getByAccount(
            @PathVariable Long bankAccountId) {
        return ResponseEntity.ok(ApiResponse.success(
                bankTransactionService.getByAccount(bankAccountId)));
    }

    @GetMapping("/account/{bankAccountId}/range")
    @PreAuthorize("hasAuthority('VIEW_BANKING')")
    public ResponseEntity<ApiResponse<List<BankTransactionResponseDto>>> getByDateRange(
            @PathVariable Long bankAccountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                bankTransactionService.getByAccountAndDateRange(bankAccountId, from, to)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_BANKING')")
    public ResponseEntity<ApiResponse<BankTransactionResponseDto>> create(
            @Valid @RequestBody BankTransactionRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Transaction created",
                bankTransactionService.create(request)));
    }

    @PatchMapping("/{id}/reconcile")
    @PreAuthorize("hasAuthority('EDIT_BANKING')")
    public ResponseEntity<ApiResponse<BankTransactionResponseDto>> reconcile(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Transaction reconciled",
                bankTransactionService.reconcile(id)));
    }

    @PatchMapping("/{id}/unreconcile")
    @PreAuthorize("hasAuthority('EDIT_BANKING')")
    public ResponseEntity<ApiResponse<BankTransactionResponseDto>> unreconcile(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Transaction un-reconciled",
                bankTransactionService.unreconcile(id)));
    }

    @PatchMapping("/{id}/void")
    @PreAuthorize("hasAuthority('EDIT_BANKING')")
    public ResponseEntity<ApiResponse<BankTransactionResponseDto>> voidTransaction(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Transaction voided",
                bankTransactionService.voidTransaction(id)));
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasAuthority('CREATE_BANKING')")
    public ResponseEntity<ApiResponse<BankTransferResponseDto>> transfer(
            @Valid @RequestBody BankTransferRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Transfer completed",
                bankTransactionService.transfer(request)));
    }
}