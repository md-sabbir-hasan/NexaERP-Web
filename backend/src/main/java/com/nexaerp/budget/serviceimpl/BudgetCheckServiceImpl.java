package com.nexaerp.budget.serviceimpl;

import com.nexaerp.account.Account;
import com.nexaerp.accountingperiod.AccountingPeriod;
import com.nexaerp.accountingperiod.AccountingPeriodRepository;
import com.nexaerp.budget.*;
import com.nexaerp.budget.dto.BudgetWarningDto;
import com.nexaerp.fiscalyear.FiscalYear;
import com.nexaerp.fiscalyear.FiscalYearRepository;
import com.nexaerp.fiscalyear.FiscalYearStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BudgetCheckServiceImpl implements BudgetCheckService {

    private final FiscalYearRepository fiscalYearRepository;
    private final AccountingPeriodRepository accountingPeriodRepository;
    private final BudgetRepository budgetRepository;
    private final BudgetLineRepository budgetLineRepository;
    private final BudgetPeriodAllocationRepository budgetPeriodAllocationRepository;
    private final BudgetActualService budgetActualService;

    @Override
    public Optional<BudgetWarningDto> checkExpenseAccount(Account account, LocalDate postingDate, BigDecimal proposedAmount) {

        // 1. Active fiscal year covering the posting date
        FiscalYear fiscalYear = fiscalYearRepository.findContainingDate(postingDate).stream()
                .filter(fy -> fy.getStatus() == FiscalYearStatus.ACTIVE)
                .findFirst()
                .orElse(null);
        if (fiscalYear == null) {
            return Optional.empty();
        }

        // 2. Active budget for that fiscal year
        Budget budget = budgetRepository
                .findByFiscalYearIdAndStatusAndDeletedAtIsNull(fiscalYear.getId(), BudgetStatus.ACTIVE)
                .orElse(null);
        if (budget == null) {
            return Optional.empty();
        }

        // 3. Budget line for this account
        BudgetLine line = budgetLineRepository
                .findByBudgetIdAndAccountId(budget.getId(), account.getId())
                .orElse(null);
        if (line == null) {
            return Optional.empty(); // no budget configured for this account â€” silent
        }

        // 4. Accounting period covering the posting date
        List<AccountingPeriod> periods = accountingPeriodRepository.findPeriodsContainingDate(postingDate);
        if (periods.isEmpty()) {
            return Optional.empty();
        }
        AccountingPeriod period = periods.get(0);

        // 5. Budgeted amount for this period
        BigDecimal budgetAmount = budgetPeriodAllocationRepository
                .findByBudgetLineIdAndAccountingPeriodId(line.getId(), period.getId())
                .map(BudgetPeriodAllocation::getBudgetAmount)
                .orElse(null);
        if (budgetAmount == null) {
            return Optional.empty();
        }

        // 6. Already-posted actual for this account within the period, then project this transaction
        BigDecimal actualBeforePosting = budgetActualService.getActualForAccount(
                account, period.getStartDate(), period.getEndDate());

        BigDecimal projectedActual = actualBeforePosting.add(proposedAmount);

        if (projectedActual.compareTo(budgetAmount) <= 0) {
            return Optional.empty(); // within budget â€” no warning
        }

        BigDecimal exceededAmount = projectedActual.subtract(budgetAmount);

        return Optional.of(BudgetWarningDto.builder()
                .budgetId(budget.getId())
                .accountId(account.getId())
                .accountCode(account.getCode())
                .accountName(account.getName())
                .accountingPeriodId(period.getId())
                .accountingPeriodName(period.getName())
                .budgetAmount(budgetAmount)
                .actualBeforePosting(actualBeforePosting)
                .transactionAmount(proposedAmount)
                .projectedActual(projectedActual)
                .exceededAmount(exceededAmount)
                .message(String.format(
                        "%s budget for %s exceeded by %s (Budget: %s, Projected: %s)",
                        account.getName(), period.getName(), exceededAmount, budgetAmount, projectedActual))
                .build());
    }
}