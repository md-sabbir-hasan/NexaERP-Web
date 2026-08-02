package com.nexaerp.journal;

import com.nexaerp.account.Account;
import com.nexaerp.approval.ApprovalService;
import com.nexaerp.account.AccountRepository;
import com.nexaerp.account.AccountType;
import com.nexaerp.accountingperiod.AccountingPeriodService;
import com.nexaerp.audit.AuditLogService;
import com.nexaerp.banking.services.BankTransactionService;
import com.nexaerp.budget.BudgetCheckService;
import com.nexaerp.costcenter.CostCenter;
import com.nexaerp.costcenter.CostCenterService;
import com.nexaerp.email.BudgetAlertEmailService;
import com.nexaerp.journal.dto.JournalEntryRequestDto;
import com.nexaerp.journal.dto.JournalLineRequestDto;
import com.nexaerp.notification.NotificationService;
import com.nexaerp.notification.NotificationModule;
import com.nexaerp.notification.NotificationPriority;
import com.nexaerp.notification.NotificationType;
import com.nexaerp.security.MakerCheckerService;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class JournalCostCenterPropagationTest {

    @Mock private JournalEntryRepository journalEntryRepository;
    @Mock private JournalLineRepository journalLineRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private AccountingPeriodService accountingPeriodService;
    @Mock private MakerCheckerService makerCheckerService;
    @Mock private BankTransactionService bankTransactionService;
    @Mock private BudgetCheckService budgetCheckService;
    @Mock private NotificationService notificationService;
    @Mock private BudgetAlertEmailService budgetAlertEmailService;
    @Mock private CostCenterService costCenterService;
    @Mock private ApprovalService approvalService;
    @InjectMocks private JournalEntryServiceImpl service;

    private Account debitAccount;
    private Account creditAccount;
    private CostCenter costCenter;

    @BeforeEach
    void setUp() {
        debitAccount = account(1L, AccountType.EXPENSE);
        creditAccount = account(2L, AccountType.LIABILITY);
        costCenter = CostCenter.builder().id(9L).code("OPS").name("Operations").isActive(true).build();
        lenient().when(journalEntryRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(invocation -> {
            JournalEntry entry = invocation.getArgument(0);
            entry.setId(10L);
            return entry;
        });
        lenient().when(accountRepository.findById(1L)).thenReturn(Optional.of(debitAccount));
        lenient().when(accountRepository.findById(2L)).thenReturn(Optional.of(creditAccount));
        lenient().when(accountRepository.existsByParentId(any())).thenReturn(false);
        lenient().when(costCenterService.resolveActive(9L)).thenReturn(costCenter);
        lenient().when(approvalService.isManualJournalApprovalEnabled()).thenReturn(false);
    }

    @Test
    void manualLinePersistsOptionalCostCenter() {
        JournalLineRequestDto debit = new JournalLineRequestDto();
        debit.setAccountId(1L);
        debit.setCostCenterId(9L);
        debit.setDebit(new BigDecimal("50.00"));
        debit.setCredit(BigDecimal.ZERO);
        JournalLineRequestDto credit = new JournalLineRequestDto();
        credit.setAccountId(2L);
        credit.setDebit(BigDecimal.ZERO);
        credit.setCredit(new BigDecimal("50.00"));

        service.create(new JournalEntryRequestDto(
                LocalDate.of(2026, 7, 20), "Allocation", JournalEntryType.GENERAL, List.of(debit, credit)));

        ArgumentCaptor<List<JournalLine>> captor = ArgumentCaptor.forClass(List.class);
        verify(journalLineRepository).saveAll(captor.capture());
        assertSame(costCenter, captor.getValue().get(0).getCostCenter());
        assertSame(null, captor.getValue().get(1).getCostCenter());
        verify(notificationService).scheduleUniqueForCurrentUserAfterCommit(
                NotificationType.JOURNAL_DRAFT_PENDING,
                NotificationPriority.MEDIUM,
                NotificationModule.JOURNAL,
                "Journal draft created",
                "Journal JE-0001 was created as a draft.",
                "/journals/10/edit",
                "JOURNAL",
                10L
        );
    }

    @Test
    void reversalCopiesCostCenter() {
        JournalEntry original = JournalEntry.builder()
                .id(20L).entryNumber("JE-0020").date(LocalDate.of(2026, 7, 20))
                .type(JournalEntryType.GENERAL).status(JournalStatus.POSTED)
                .sourceType(JournalSourceType.MANUAL).totalAmount(new BigDecimal("50.00"))
                .build();
        original.setLines(List.of(
                JournalLine.builder().journalEntry(original).account(debitAccount).costCenter(costCenter)
                        .debit(new BigDecimal("50.00")).credit(BigDecimal.ZERO).description("Allocation").build()));
        when(journalEntryRepository.findById(20L)).thenReturn(Optional.of(original));

        service.reverse(20L);

        ArgumentCaptor<List<JournalLine>> captor = ArgumentCaptor.forClass(List.class);
        verify(journalLineRepository).saveAll(captor.capture());
        assertSame(costCenter, captor.getValue().get(0).getCostCenter());
        verify(notificationService, never()).scheduleUniqueForCurrentUserAfterCommit(
                any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void editingExistingDraftDoesNotCreateAnotherNotification() {
        JournalEntry draft = JournalEntry.builder()
                .id(20L)
                .entryNumber("JE-0020")
                .date(LocalDate.of(2026, 7, 20))
                .description("Original")
                .type(JournalEntryType.GENERAL)
                .status(JournalStatus.DRAFT)
                .sourceType(JournalSourceType.MANUAL)
                .totalAmount(new BigDecimal("50.00"))
                .build();
        draft.setLines(List.of(
                JournalLine.builder().journalEntry(draft).account(debitAccount)
                        .debit(new BigDecimal("50.00")).credit(BigDecimal.ZERO).build(),
                JournalLine.builder().journalEntry(draft).account(creditAccount)
                        .debit(BigDecimal.ZERO).credit(new BigDecimal("50.00")).build()
        ));
        when(journalEntryRepository.findById(20L)).thenReturn(Optional.of(draft));

        JournalLineRequestDto debit = new JournalLineRequestDto();
        debit.setAccountId(1L);
        debit.setDebit(new BigDecimal("60.00"));
        debit.setCredit(BigDecimal.ZERO);
        JournalLineRequestDto credit = new JournalLineRequestDto();
        credit.setAccountId(2L);
        credit.setDebit(BigDecimal.ZERO);
        credit.setCredit(new BigDecimal("60.00"));

        service.update(20L, new JournalEntryRequestDto(
                LocalDate.of(2026, 7, 21), "Updated", JournalEntryType.GENERAL,
                List.of(debit, credit)));

        verify(notificationService, never()).scheduleUniqueForCurrentUserAfterCommit(
                any(), any(), any(), any(), any(), any(), any(), any()
        );
        verify(journalEntryRepository, times(1)).save(draft);
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
