package com.nexaerp.recurringexpense;

import com.nexaerp.common.response.ApiResponse;
import com.nexaerp.recurringexpense.dto.RecurringExpenseTemplateRequestDto;
import com.nexaerp.recurringexpense.dto.RecurringExpenseTemplateResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recurring-expenses")
@RequiredArgsConstructor
public class RecurringExpenseTemplateController {

    private final RecurringExpenseTemplateService recurringExpenseTemplateService;

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_RECURRING_EXPENSE')")
    public ResponseEntity<ApiResponse<RecurringExpenseTemplateResponseDto>> create(
            @Valid @RequestBody RecurringExpenseTemplateRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Recurring expense template created",
                recurringExpenseTemplateService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EDIT_RECURRING_EXPENSE')")
    public ResponseEntity<ApiResponse<RecurringExpenseTemplateResponseDto>> update(
            @PathVariable Long id, @Valid @RequestBody RecurringExpenseTemplateRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Recurring expense template updated",
                recurringExpenseTemplateService.update(id, request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VIEW_RECURRING_EXPENSE')")
    public ResponseEntity<ApiResponse<RecurringExpenseTemplateResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(recurringExpenseTemplateService.getById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_RECURRING_EXPENSE')")
    public ResponseEntity<ApiResponse<List<RecurringExpenseTemplateResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(recurringExpenseTemplateService.getAll()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('EDIT_RECURRING_EXPENSE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        recurringExpenseTemplateService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Recurring expense template deleted", null));
    }

    @PostMapping("/{id}/pause")
    @PreAuthorize("hasAuthority('EDIT_RECURRING_EXPENSE')")
    public ResponseEntity<ApiResponse<RecurringExpenseTemplateResponseDto>> pause(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Template paused", recurringExpenseTemplateService.pause(id)));
    }

    @PostMapping("/{id}/resume")
    @PreAuthorize("hasAuthority('EDIT_RECURRING_EXPENSE')")
    public ResponseEntity<ApiResponse<RecurringExpenseTemplateResponseDto>> resume(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Template resumed", recurringExpenseTemplateService.resume(id)));
    }

    @PostMapping("/{id}/run-now")
    @PreAuthorize("hasAuthority('EDIT_RECURRING_EXPENSE')")
    public ResponseEntity<ApiResponse<RecurringExpenseTemplateResponseDto>> runNow(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Expense generated", recurringExpenseTemplateService.runNow(id)));
    }
}