package com.nexaerp.accountingperiod.checklist;

import com.nexaerp.accountingperiod.dto.PeriodCloseCheckItemDto;
import com.nexaerp.accountingperiod.dto.PeriodCloseCheckResultDto;
import com.nexaerp.expense.Expense;
import com.nexaerp.expense.ExpenseRepository;
import com.nexaerp.expense.ExpenseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UnpostedExpenseCheck implements PeriodCloseCheck {

    private final ExpenseRepository expenseRepository;

    @Override
    public String getCode() {
        return "UNPOSTED_EXPENSE";
    }

    @Override
    public String getName() {
        return "Unposted Expense";
    }

    @Override
    public PeriodCloseCheckResultDto run(LocalDate periodEndDate) {
        List<Expense> drafts =
                expenseRepository.findByStatusAndExpenseDateLessThanEqual(ExpenseStatus.DRAFT, periodEndDate);

        List<PeriodCloseCheckItemDto> items = drafts.stream()
                .map(e -> PeriodCloseCheckItemDto.builder()
                        .id(e.getId())
                        .reference(e.getExpenseNumber())
                        .date(e.getExpenseDate())
                        .amount(e.getAmount())
                        .build())
                .toList();

        return PeriodCloseCheckResultDto.builder()
                .code(getCode())
                .name(getName())
                .passed(items.isEmpty())
                .count(items.size())
                .items(items)
                .build();
    }
}