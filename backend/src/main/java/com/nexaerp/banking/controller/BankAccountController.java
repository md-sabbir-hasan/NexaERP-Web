package com.nexaerp.banking.controller;


import com.nexaerp.banking.dto.BankAccountRequestDto;
import com.nexaerp.banking.dto.BankAccountResponseDto;
import com.nexaerp.banking.enums.BankAccountType;
import com.nexaerp.banking.services.BankAccountService;
import com.nexaerp.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bank-accounts")
@RequiredArgsConstructor
public class BankAccountController {
    private final BankAccountService bankAccountService;

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_BANKING')")
    public ResponseEntity<ApiResponse<List<BankAccountResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(bankAccountService.getAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VIEW_BANKING')")
    public ResponseEntity<ApiResponse<BankAccountResponseDto>> getById(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(bankAccountService.getById(id)));
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("hasAuthority('VIEW_BANKING')")
    public ResponseEntity<ApiResponse<List<BankAccountResponseDto>>> getByType(
            @PathVariable BankAccountType type) {
        return ResponseEntity.ok(ApiResponse.success(bankAccountService.getByType(type)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_BANKING')")
    public ResponseEntity<ApiResponse<BankAccountResponseDto>> create(
            @Valid @RequestBody BankAccountRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Bank account created",
                bankAccountService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EDIT_BANKING')")
    public ResponseEntity<ApiResponse<BankAccountResponseDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody BankAccountRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Bank account updated",
                bankAccountService.update(id, request)));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('EDIT_BANKING')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        bankAccountService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success("Bank account deactivated", null));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('EDIT_BANKING')")
    public ResponseEntity<ApiResponse<Void>> activate(@PathVariable Long id) {
        bankAccountService.activate(id);
        return ResponseEntity.ok(ApiResponse.success("Bank account activated", null));
    }
}
