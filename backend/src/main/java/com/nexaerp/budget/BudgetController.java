package com.nexaerp.budget;

import com.nexaerp.budget.dto.*;
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
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_BUDGET')")
    public ResponseEntity<ApiResponse<BudgetResponseDto>> create(@Valid @RequestBody BudgetCreateRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Budget created", budgetService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EDIT_BUDGET')")
    public ResponseEntity<ApiResponse<BudgetResponseDto>> update(
            @PathVariable Long id, @Valid @RequestBody BudgetUpdateRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Budget updated", budgetService.update(id, request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VIEW_BUDGET')")
    public ResponseEntity<ApiResponse<BudgetResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(budgetService.getById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_BUDGET')")
    public ResponseEntity<ApiResponse<List<BudgetResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(budgetService.getAll()));
    }

    @GetMapping("/fiscal-year/{fiscalYearId}")
    @PreAuthorize("hasAuthority('VIEW_BUDGET')")
    public ResponseEntity<ApiResponse<List<BudgetResponseDto>>> getByFiscalYear(@PathVariable Long fiscalYearId) {
        return ResponseEntity.ok(ApiResponse.success(budgetService.getByFiscalYear(fiscalYearId)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE_BUDGET')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        budgetService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Budget deleted", null));
    }

    @PostMapping("/{id}/lines")
    @PreAuthorize("hasAuthority('EDIT_BUDGET')")
    public ResponseEntity<ApiResponse<BudgetLineResponseDto>> addLine(
            @PathVariable Long id, @Valid @RequestBody BudgetLineRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Budget line added", budgetService.addLine(id, request)));
    }

    @PutMapping("/{id}/lines/{lineId}")
    @PreAuthorize("hasAuthority('EDIT_BUDGET')")
    public ResponseEntity<ApiResponse<BudgetLineResponseDto>> updateLine(
            @PathVariable Long id, @PathVariable Long lineId, @Valid @RequestBody BudgetLineRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Budget line updated", budgetService.updateLine(id, lineId, request)));
    }

    @DeleteMapping("/{id}/lines/{lineId}")
    @PreAuthorize("hasAuthority('EDIT_BUDGET')")
    public ResponseEntity<ApiResponse<Void>> deleteLine(@PathVariable Long id, @PathVariable Long lineId) {
        budgetService.deleteLine(id, lineId);
        return ResponseEntity.ok(ApiResponse.success("Budget line deleted", null));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('ACTIVATE_BUDGET')")
    public ResponseEntity<ApiResponse<BudgetResponseDto>> activate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Budget activated", budgetService.activate(id)));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('CLOSE_BUDGET')")
    public ResponseEntity<ApiResponse<BudgetResponseDto>> close(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Budget closed", budgetService.close(id)));
    }

    @GetMapping("/{id}/variance")
    @PreAuthorize("hasAuthority('VIEW_BUDGET_REPORT')")
    public ResponseEntity<ApiResponse<BudgetVarianceResponseDto>> getVariance(
            @PathVariable Long id,
            @RequestParam(required = false) Long periodId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(ApiResponse.success(budgetService.getVariance(id, periodId, fromDate, toDate)));
    }
}