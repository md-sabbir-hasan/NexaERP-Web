package com.nexaerp.expense;

import com.nexaerp.common.response.ApiResponse;
import com.nexaerp.expense.dto.ExpenseCancelRequestDto;
import com.nexaerp.expense.dto.ExpenseRequestDto;
import com.nexaerp.expense.dto.ExpenseResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_EXPENSE')")
    public ResponseEntity<ApiResponse<ExpenseResponseDto>> create(
            @Valid @RequestBody ExpenseRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Expense recorded", expenseService.create(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VIEW_EXPENSE')")
    public ResponseEntity<ApiResponse<ExpenseResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(expenseService.getById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_EXPENSE')")
    public ResponseEntity<ApiResponse<List<ExpenseResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(expenseService.getAll()));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('CANCEL_EXPENSE')")
    public ResponseEntity<ApiResponse<ExpenseResponseDto>> cancel(
            @PathVariable Long id, @Valid @RequestBody ExpenseCancelRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Expense cancelled", expenseService.cancel(id, request)));
    }

    @PostMapping("/{id}/post")
    @PreAuthorize("hasAuthority('CREATE_EXPENSE')")
    public ResponseEntity<ApiResponse<ExpenseResponseDto>> post(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Expense posted", expenseService.post(id)));
    }
}