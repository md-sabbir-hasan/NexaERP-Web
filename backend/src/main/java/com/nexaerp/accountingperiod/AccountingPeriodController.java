package com.nexaerp.accountingperiod;

import com.nexaerp.accountingperiod.dto.AccountingPeriodRequestDto;
import com.nexaerp.accountingperiod.dto.AccountingPeriodResponseDto;
import com.nexaerp.accountingperiod.dto.PeriodCloseChecklistResponseDto;
import com.nexaerp.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/accounting-periods")
@RequiredArgsConstructor
public class AccountingPeriodController {

    private final AccountingPeriodService accountingPeriodService;

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_ACCOUNTING_PERIOD')")
    public ResponseEntity<ApiResponse<List<AccountingPeriodResponseDto>>> getAll(
            @RequestParam(required = false) Long fiscalYearId
    ) {
        return ResponseEntity.ok(ApiResponse.success(accountingPeriodService.getAll(fiscalYearId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VIEW_ACCOUNTING_PERIOD')")
    public ResponseEntity<ApiResponse<AccountingPeriodResponseDto>> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.success(accountingPeriodService.getById(id)));
    }

    @GetMapping("/current")
    @PreAuthorize("hasAuthority('VIEW_ACCOUNTING_PERIOD')")
    public ResponseEntity<ApiResponse<AccountingPeriodResponseDto>> getCurrent(
            @RequestParam(required = false) LocalDate date
    ) {
        return ResponseEntity.ok(ApiResponse.success(accountingPeriodService.getCurrent(date)));
    }

    @GetMapping("/{id}/close-checklist")
    @PreAuthorize("hasAuthority('VIEW_ACCOUNTING_PERIOD')")
    public ResponseEntity<ApiResponse<PeriodCloseChecklistResponseDto>> getCloseChecklist(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.success(accountingPeriodService.getCloseChecklist(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_ACCOUNTING_PERIOD')")
    public ResponseEntity<ApiResponse<AccountingPeriodResponseDto>> create(
            @Valid @RequestBody AccountingPeriodRequestDto request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Accounting period created",
                accountingPeriodService.create(request)
        ));
    }

    @PostMapping("/generate/{fiscalYearId}")
    @PreAuthorize("hasAuthority('CREATE_ACCOUNTING_PERIOD')")
    public ResponseEntity<ApiResponse<List<AccountingPeriodResponseDto>>> generate(
            @PathVariable Long fiscalYearId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Accounting periods generated",
                accountingPeriodService.generateMonthlyPeriods(fiscalYearId)
        ));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EDIT_ACCOUNTING_PERIOD')")
    public ResponseEntity<ApiResponse<AccountingPeriodResponseDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody AccountingPeriodRequestDto request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Accounting period updated",
                accountingPeriodService.update(id, request)
        ));
    }

    @PatchMapping("/{id}/open")
    @PreAuthorize("hasAuthority('OPEN_ACCOUNTING_PERIOD')")
    public ResponseEntity<ApiResponse<AccountingPeriodResponseDto>> open(
            @PathVariable Long id,
            @RequestParam(required = false) String remarks
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Accounting period opened",
                accountingPeriodService.open(id, remarks)
        ));
    }

    @PatchMapping("/{id}/close")
    @PreAuthorize("hasAuthority('CLOSE_ACCOUNTING_PERIOD')")
    public ResponseEntity<ApiResponse<AccountingPeriodResponseDto>> close(
            @PathVariable Long id,
            @RequestParam(required = false) String remarks
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Accounting period closed",
                accountingPeriodService.close(id, remarks)
        ));
    }

    @PatchMapping("/{id}/lock")
    @PreAuthorize("hasAuthority('LOCK_ACCOUNTING_PERIOD')")
    public ResponseEntity<ApiResponse<AccountingPeriodResponseDto>> lock(
            @PathVariable Long id,
            @RequestParam(required = false) String remarks
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Accounting period locked",
                accountingPeriodService.lock(id, remarks)
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE_ACCOUNTING_PERIOD')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        accountingPeriodService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Accounting period deleted", null));
    }
}