package com.nexaerp.budget.serviceimpl;

import com.nexaerp.account.Account;
import com.nexaerp.account.AccountType;
import com.nexaerp.budget.AccountActualProjection;
import com.nexaerp.budget.BudgetActualRepository;
import com.nexaerp.budget.BudgetActualService;
import com.nexaerp.journal.JournalStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetActualServiceImpl implements BudgetActualService {

    private final BudgetActualRepository budgetActualRepository;

    @Override
    public BigDecimal getActualForAccount(Account account, LocalDate fromDate, LocalDate toDate) {
        Map<Long, BigDecimal> result = getActualByAccounts(List.of(account), fromDate, toDate);
        return result.getOrDefault(account.getId(), BigDecimal.ZERO);
    }

    @Override
    public Map<Long, BigDecimal> getActualByAccounts(List<Account> accounts, LocalDate fromDate, LocalDate toDate) {
        if (accounts == null || accounts.isEmpty()) {
            return Map.of();
        }

        Map<Long, AccountType> typeByAccountId = accounts.stream()
                .collect(Collectors.toMap(Account::getId, Account::getType, (a, b) -> a));

        List<Long> accountIds = List.copyOf(typeByAccountId.keySet());

        List<AccountActualProjection> projections = budgetActualRepository.findAccountActuals(
                accountIds, fromDate, toDate, JournalStatus.POSTED);

        Map<Long, BigDecimal> actuals = new HashMap<>();

        // default zero for accounts with no journal activity in range
        for (Long accountId : accountIds) {
            actuals.put(accountId, BigDecimal.ZERO);
        }

        for (AccountActualProjection p : projections) {
            AccountType type = typeByAccountId.get(p.getAccountId());
            BigDecimal debit = p.getTotalDebit() != null ? p.getTotalDebit() : BigDecimal.ZERO;
            BigDecimal credit = p.getTotalCredit() != null ? p.getTotalCredit() : BigDecimal.ZERO;

            // Natural balance: Expense = Debit - Credit, Revenue = Credit - Debit
            BigDecimal actual = (type == AccountType.REVENUE)
                    ? credit.subtract(debit)
                    : debit.subtract(credit);

            actuals.put(p.getAccountId(), actual);
        }

        return actuals;
    }
}