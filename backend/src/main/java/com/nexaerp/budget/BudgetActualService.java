package com.nexaerp.budget;

import com.nexaerp.account.Account;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface BudgetActualService {

    BigDecimal getActualForAccount(Account account, LocalDate fromDate, LocalDate toDate);

    Map<Long, BigDecimal> getActualByAccounts(List<Account> accounts, LocalDate fromDate, LocalDate toDate);
}