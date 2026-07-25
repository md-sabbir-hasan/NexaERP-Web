package com.nexaerp.budget;

import com.nexaerp.budget.dto.*;

import java.time.LocalDate;
import java.util.List;

public interface BudgetService {
    BudgetResponseDto create(BudgetCreateRequestDto request);
    BudgetResponseDto update(Long id, BudgetUpdateRequestDto request);
    BudgetResponseDto getById(Long id);
    List<BudgetResponseDto> getAll();
    List<BudgetResponseDto> getByFiscalYear(Long fiscalYearId);
    void delete(Long id);

    BudgetLineResponseDto addLine(Long budgetId, BudgetLineRequestDto request);
    BudgetLineResponseDto updateLine(Long budgetId, Long lineId, BudgetLineRequestDto request);
    void deleteLine(Long budgetId, Long lineId);

    BudgetResponseDto activate(Long id);
    BudgetResponseDto close(Long id);

    BudgetVarianceResponseDto getVariance(Long budgetId, Long periodId, LocalDate fromDate, LocalDate toDate);
}