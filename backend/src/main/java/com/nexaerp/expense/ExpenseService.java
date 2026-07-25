package com.nexaerp.expense;

import com.nexaerp.expense.dto.ExpenseCancelRequestDto;
import com.nexaerp.expense.dto.ExpenseRequestDto;
import com.nexaerp.expense.dto.ExpenseResponseDto;

import java.util.List;

public interface ExpenseService {
    ExpenseResponseDto create(ExpenseRequestDto request);
    ExpenseResponseDto createFromRecurringTemplate(
            ExpenseRequestDto request,
            Long recurringTemplateId
    );
    ExpenseResponseDto getById(Long id);
    List<ExpenseResponseDto> getAll();
    ExpenseResponseDto cancel(Long id, ExpenseCancelRequestDto request);
    ExpenseResponseDto attachReceipt(Long id, String attachmentUrl);
    ExpenseResponseDto post(Long id);
}