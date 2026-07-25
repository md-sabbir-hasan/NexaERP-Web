package com.nexaerp.budget;

import com.nexaerp.account.Account;
import com.nexaerp.account.AccountType;
import com.nexaerp.budget.serviceimpl.BudgetActualServiceImpl;
import com.nexaerp.journal.JournalStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BudgetActualServiceImplTest {
    private final BudgetActualRepository repository = mock(BudgetActualRepository.class);
    private final BudgetActualServiceImpl service = new BudgetActualServiceImpl(repository);

    @Test void expenseUsesDebitMinusCreditAndIncludesPostedAndReversed() {
        Account expense = account(1L, AccountType.EXPENSE);
        AccountActualProjection projection = projection(1L, "120", "20");
        when(repository.findAccountActuals(anyList(), any(), any(), anyList()))
                .thenReturn(List.of(projection));
        assertThat(service.getActualForAccount(expense, LocalDate.parse("2026-01-01"),
                LocalDate.parse("2026-01-31"))).isEqualByComparingTo("100");
        verify(repository).findAccountActuals(anyList(), any(), any(),
                eq(List.of(JournalStatus.POSTED, JournalStatus.REVERSED)));
    }

    @Test void revenueUsesCreditMinusDebit() {
        Account revenue = account(2L, AccountType.REVENUE);
        AccountActualProjection projection = projection(2L, "15", "115");
        when(repository.findAccountActuals(anyList(), any(), any(), anyList()))
                .thenReturn(List.of(projection));
        assertThat(service.getActualForAccount(revenue, LocalDate.MIN, LocalDate.MAX))
                .isEqualByComparingTo("100");
    }

    @Test void negativeActualIsPreserved() {
        Account expense = account(1L, AccountType.EXPENSE);
        AccountActualProjection projection = projection(1L, "10", "30");
        when(repository.findAccountActuals(anyList(), any(), any(), anyList()))
                .thenReturn(List.of(projection));
        assertThat(service.getActualForAccount(expense, LocalDate.MIN, LocalDate.MAX))
                .isEqualByComparingTo("-20");
    }

    @Test void accountWithoutActivityReturnsZero() {
        when(repository.findAccountActuals(anyList(), any(), any(), anyList())).thenReturn(List.of());
        assertThat(service.getActualForAccount(account(1L, AccountType.EXPENSE),
                LocalDate.MIN, LocalDate.MAX)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test void samePeriodOriginalAndPostedReversalNetToZero() {
        AccountActualProjection projection = projection(1L, "100", "100");
        when(repository.findAccountActuals(anyList(), any(), any(), anyList()))
                .thenReturn(List.of(projection));
        assertThat(service.getActualForAccount(account(1L, AccountType.EXPENSE),
                LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31")))
                .isEqualByComparingTo("0");
    }

    @Test void crossPeriodReversalAffectsEachSelectedDateRange() {
        AccountActualProjection original = projection(1L, "100", "0");
        AccountActualProjection reversal = projection(1L, "0", "100");
        when(repository.findAccountActuals(anyList(), eq(LocalDate.parse("2026-01-01")),
                eq(LocalDate.parse("2026-01-31")), anyList())).thenReturn(List.of(original));
        when(repository.findAccountActuals(anyList(), eq(LocalDate.parse("2026-02-01")),
                eq(LocalDate.parse("2026-02-28")), anyList())).thenReturn(List.of(reversal));
        Account expense = account(1L, AccountType.EXPENSE);
        assertThat(service.getActualForAccount(expense, LocalDate.parse("2026-01-01"),
                LocalDate.parse("2026-01-31"))).isEqualByComparingTo("100");
        assertThat(service.getActualForAccount(expense, LocalDate.parse("2026-02-01"),
                LocalDate.parse("2026-02-28"))).isEqualByComparingTo("-100");
    }

    private Account account(Long id, AccountType type) {
        return new Account(id, "A" + id, "Account", null, type, true, false,
                false, BigDecimal.ZERO, null, List.of());
    }

    private AccountActualProjection projection(Long id, String debit, String credit) {
        AccountActualProjection value = mock(AccountActualProjection.class);
        when(value.getAccountId()).thenReturn(id);
        when(value.getTotalDebit()).thenReturn(new BigDecimal(debit));
        when(value.getTotalCredit()).thenReturn(new BigDecimal(credit));
        return value;
    }
}
