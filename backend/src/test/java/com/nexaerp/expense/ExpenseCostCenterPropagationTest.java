package com.nexaerp.expense;

import com.nexaerp.account.Account;
import com.nexaerp.account.AccountRepository;
import com.nexaerp.account.AccountType;
import com.nexaerp.accountingperiod.AccountingPeriodService;
import com.nexaerp.audit.AuditLogService;
import com.nexaerp.banking.services.BankTransactionService;
import com.nexaerp.budget.BudgetCheckService;
import com.nexaerp.costcenter.CostCenter;
import com.nexaerp.costcenter.CostCenterService;
import com.nexaerp.email.BudgetAlertEmailService;
import com.nexaerp.expense.dto.ExpenseRequestDto;
import com.nexaerp.journal.JournalEntry;
import com.nexaerp.journal.JournalEntryRepository;
import com.nexaerp.journal.JournalLine;
import com.nexaerp.journal.JournalLineRepository;
import com.nexaerp.notification.NotificationService;
import com.nexaerp.notification.NotificationModule;
import com.nexaerp.notification.NotificationPriority;
import com.nexaerp.notification.NotificationType;
import com.nexaerp.party.PartyRepository;
import com.nexaerp.payment.PaymentAllocationRepository;
import com.nexaerp.settings.SystemSettingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseCostCenterPropagationTest {

    @Mock private ExpenseRepository expenseRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private PartyRepository partyRepository;
    @Mock private JournalEntryRepository journalEntryRepository;
    @Mock private JournalLineRepository journalLineRepository;
    @Mock private AccountingPeriodService accountingPeriodService;
    @Mock private SystemSettingsService systemSettingsService;
    @Mock private BankTransactionService bankTransactionService;
    @Mock private PaymentAllocationRepository paymentAllocationRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private BudgetCheckService budgetCheckService;
    @Mock private NotificationService notificationService;
    @Mock private BudgetAlertEmailService budgetAlertEmailService;
    @Mock private CostCenterService costCenterService;
    @InjectMocks private ExpenseServiceImpl service;

    @Test
    void expenseCostCenterPropagatesOnlyToDebitLine() {
        Account expenseAccount = account(1L, AccountType.EXPENSE);
        Account paymentAccount = account(2L, AccountType.ASSET);
        CostCenter costCenter = CostCenter.builder()
                .id(7L).code("OPS").name("Operations").isActive(true).build();
        when(accountRepository.findById(1L)).thenReturn(Optional.of(expenseAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(paymentAccount));
        when(costCenterService.resolveActive(7L)).thenReturn(costCenter);
        when(expenseRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
            Expense expense = invocation.getArgument(0);
            expense.setId(10L);
            return expense;
        });
        when(journalEntryRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(budgetCheckService.checkExpenseAccount(any(), any(), any())).thenReturn(Optional.empty());

        ExpenseRequestDto request = new ExpenseRequestDto();
        request.setExpenseDate(LocalDate.of(2026, 7, 20));
        request.setExpenseAccountId(1L);
        request.setCostCenterId(7L);
        request.setPaidImmediately(true);
        request.setPaymentAccountId(2L);
        request.setAmount(new BigDecimal("100.00"));

        service.create(request);

        ArgumentCaptor<JournalLine> captor = ArgumentCaptor.forClass(JournalLine.class);
        verify(journalLineRepository, times(2)).save(captor.capture());
        List<JournalLine> lines = captor.getAllValues();
        assertSame(costCenter, lines.get(0).getCostCenter());
        assertTrue(lines.get(1).getCostCenter() == null);
        verify(notificationService, never()).scheduleUniqueForCurrentUserAfterCommit(
                any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void recurringTemplateCreatesOneDraftNotification() {
        Account expenseAccount = account(1L, AccountType.EXPENSE);
        Account paymentAccount = account(2L, AccountType.ASSET);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(expenseAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(paymentAccount));
        when(expenseRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
            Expense expense = invocation.getArgument(0);
            expense.setId(10L);
            return expense;
        });

        ExpenseRequestDto request = new ExpenseRequestDto();
        request.setExpenseDate(LocalDate.of(2026, 7, 20));
        request.setExpenseAccountId(1L);
        request.setPaidImmediately(true);
        request.setPaymentAccountId(2L);
        request.setAmount(new BigDecimal("100.00"));

        service.createFromRecurringTemplate(request, 99L);

        verify(notificationService).scheduleUniqueForCurrentUserAfterCommit(
                NotificationType.RECURRING_EXPENSE_DRAFT_PENDING,
                NotificationPriority.MEDIUM,
                NotificationModule.EXPENSE,
                "Recurring expense draft created",
                "Expense EXP-0001 was generated as a draft.",
                "/expense/10",
                "EXPENSE",
                10L
        );
        verify(journalEntryRepository, never()).save(any());
    }

    private Account account(Long id, AccountType type) {
        Account account = new Account();
        account.setId(id);
        account.setCode("A" + id);
        account.setName("Account " + id);
        account.setType(type);
        account.setIsActive(true);
        account.setCurrentBalance(BigDecimal.ZERO);
        return account;
    }
}
