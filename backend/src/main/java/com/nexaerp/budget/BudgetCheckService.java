package com.nexaerp.budget;

import com.nexaerp.account.Account;
import com.nexaerp.budget.dto.BudgetWarningDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface BudgetCheckService {

    /**
     * Checks whether posting {proposedAmount} against {account} on {postingDate}
     * would exceed that account's budgeted amount for the covering accounting period.
     * Returns empty if there's no active budget/period/line for this account —
     * budget enforcement is entirely optional/silent when not configured.
     */
    Optional<BudgetWarningDto> checkExpenseAccount(Account account, LocalDate postingDate, BigDecimal proposedAmount);
}