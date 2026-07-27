package com.nexaerp.email;

import com.nexaerp.budget.dto.BudgetWarningDto;
import com.nexaerp.email.dto.EmailDto;
import com.nexaerp.security.CurrentUserService;
import com.nexaerp.user.User;
import com.nexaerp.user.UserRepository;
import com.nexaerp.user.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetAlertEmailServiceImplTest {

    @Mock private EmailService emailService;
    @Mock private CurrentUserService currentUserService;
    @Mock private UserRepository userRepository;

    @InjectMocks private BudgetAlertEmailServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "frontendUrl", "http://localhost:4200/");
    }

    @AfterEach
    void cleanTransactionState() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void featureDisabledDoesNotResolveRecipientOrRegisterEmail() {
        ReflectionTestUtils.setField(service, "enabled", false);

        service.scheduleAfterCommit(
                "Expense", 1L, "EXP-001", LocalDate.of(2026, 7, 20), List.of(warning(7L, 10L)));

        verify(currentUserService, never()).getCurrentUserId();
        verify(emailService, never()).sendEmail(any());
    }

    @Test
    void emptyWarningsDoNothing() {
        service.scheduleAfterCommit(
                "Expense", 1L, "EXP-001", LocalDate.of(2026, 7, 20), List.of());

        verify(currentUserService, never()).getCurrentUserId();
        verify(emailService, never()).sendEmail(any());
    }

    @Test
    void blankEmailDoesNotRegisterDelivery() {
        mockRecipient(UserStatus.ACTIVE, " ");
        beginTransaction();

        service.scheduleAfterCommit(
                "Expense", 1L, "EXP-001", LocalDate.of(2026, 7, 20), List.of(warning(7L, 10L)));

        assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty());
        verify(emailService, never()).sendEmail(any());
    }

    @Test
    void nonActiveUserDoesNotRegisterDelivery() {
        mockRecipient(UserStatus.INACTIVE, "poster@example.com");
        beginTransaction();

        service.scheduleAfterCommit(
                "Expense", 1L, "EXP-001", LocalDate.of(2026, 7, 20), List.of(warning(7L, 10L)));

        assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty());
    }

    @Test
    void oneWarningIsSentOnlyAfterSuccessfulCommitWithRequiredContent() {
        mockRecipient(UserStatus.ACTIVE, "poster@example.com");
        beginTransaction();

        service.scheduleAfterCommit(
                "Vendor Bill",
                5L,
                "BILL-2026-000005",
                LocalDate.of(2026, 7, 20),
                List.of(warning(7L, 10L))
        );

        verify(emailService, never()).sendEmail(any());
        triggerCommit();

        ArgumentCaptor<EmailDto> captor = ArgumentCaptor.forClass(EmailDto.class);
        verify(emailService).sendEmail(captor.capture());
        EmailDto email = captor.getValue();

        assertEquals("poster@example.com", email.getTo());
        assertEquals("Budget exceeded - Vendor Bill BILL-2026-000005", email.getSubject());
        assertTrue(email.getBody().contains("The posting succeeded but exceeded one or more budgets."));
        assertTrue(email.getBody().contains("Document type: Vendor Bill"));
        assertTrue(email.getBody().contains("Document number: BILL-2026-000005"));
        assertTrue(email.getBody().contains("Posting date: 2026-07-20"));
        assertTrue(email.getBody().contains("Posted by: Poster Name"));
        assertTrue(email.getBody().contains("Account code: 5100"));
        assertTrue(email.getBody().contains("Account name: Office Expense"));
        assertTrue(email.getBody().contains("Accounting period: July 2026"));
        assertTrue(email.getBody().contains("Budget amount: 100.00"));
        assertTrue(email.getBody().contains("Actual before posting: 90.00"));
        assertTrue(email.getBody().contains("Transaction amount: 20.00"));
        assertTrue(email.getBody().contains("Projected actual: 110.00"));
        assertTrue(email.getBody().contains("Exceeded amount: 10.00"));
        assertTrue(email.getBody().contains(
                "Budget variance: http://localhost:4200/budget/7/variance"));
        assertTrue(email.getBody().contains("The posting was not blocked."));
    }

    @Test
    void multipleWarningsProduceOneConsolidatedEmailAndDuplicatesAreRemoved() {
        mockRecipient(UserStatus.ACTIVE, "poster@example.com");
        beginTransaction();
        BudgetWarningDto first = warning(7L, 10L);
        BudgetWarningDto duplicate = warning(7L, 10L);
        BudgetWarningDto second = warning(7L, 11L);
        second.setAccountCode("5200");
        second.setAccountName("Travel Expense");

        service.scheduleAfterCommit(
                "Journal",
                8L,
                "JE-0008",
                LocalDate.of(2026, 7, 21),
                Arrays.asList(first, null, duplicate, second)
        );
        triggerCommit();

        ArgumentCaptor<EmailDto> captor = ArgumentCaptor.forClass(EmailDto.class);
        verify(emailService, times(1)).sendEmail(captor.capture());
        String body = captor.getValue().getBody();
        assertEquals(1, occurrences(body, "Account code: 5100"));
        assertEquals(1, occurrences(body, "Account code: 5200"));
    }

    @Test
    void rollbackDoesNotSendEmail() {
        mockRecipient(UserStatus.ACTIVE, "poster@example.com");
        beginTransaction();

        service.scheduleAfterCommit(
                "Expense", 1L, "EXP-001", LocalDate.of(2026, 7, 20), List.of(warning(7L, 10L)));
        triggerRollback();

        verify(emailService, never()).sendEmail(any());
    }

    @Test
    void missingTransactionSynchronizationSkipsWithoutSending() {
        mockRecipient(UserStatus.ACTIVE, "poster@example.com");

        service.scheduleAfterCommit(
                "Expense", 1L, "EXP-001", LocalDate.of(2026, 7, 20), List.of(warning(7L, 10L)));

        verify(emailService, never()).sendEmail(any());
        assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
    }

    @Test
    void smtpAndRuntimeFailuresAfterCommitDoNotEscape() {
        mockRecipient(UserStatus.ACTIVE, "poster@example.com");
        beginTransaction();
        doThrow(new MailSendException("SMTP unavailable"))
                .when(emailService).sendEmail(any());

        service.scheduleAfterCommit(
                "Expense", 1L, "EXP-001", LocalDate.of(2026, 7, 20), List.of(warning(7L, 10L)));

        assertDoesNotThrow(this::triggerCommit);

        cleanTransactionState();
        mockRecipient(UserStatus.ACTIVE, "poster@example.com");
        beginTransaction();
        doThrow(new IllegalStateException("Unexpected failure"))
                .when(emailService).sendEmail(any());
        service.scheduleAfterCommit(
                "Expense", 2L, "EXP-002", LocalDate.of(2026, 7, 20), List.of(warning(7L, 10L)));

        assertDoesNotThrow(this::triggerCommit);
    }

    private void mockRecipient(UserStatus status, String email) {
        User user = User.builder()
                .id(99L)
                .name("Poster\nName")
                .email(email)
                .status(status)
                .build();
        when(currentUserService.getCurrentUserId()).thenReturn(99L);
        when(userRepository.findById(99L)).thenReturn(Optional.of(user));
    }

    private BudgetWarningDto warning(Long budgetId, Long accountId) {
        return BudgetWarningDto.builder()
                .budgetId(budgetId)
                .accountId(accountId)
                .accountCode("5100")
                .accountName("Office\nExpense")
                .accountingPeriodId(3L)
                .accountingPeriodName("July 2026")
                .budgetAmount(new BigDecimal("100.00"))
                .actualBeforePosting(new BigDecimal("90.00"))
                .transactionAmount(new BigDecimal("20.00"))
                .projectedActual(new BigDecimal("110.00"))
                .exceededAmount(new BigDecimal("10.00"))
                .message("Budget exceeded")
                .build();
    }

    private void beginTransaction() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
    }

    private void triggerCommit() {
        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
        synchronizations.forEach(synchronization -> synchronization.beforeCommit(false));
        synchronizations.forEach(TransactionSynchronization::beforeCompletion);
        synchronizations.forEach(TransactionSynchronization::afterCommit);
        synchronizations.forEach(synchronization ->
                synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
    }

    private void triggerRollback() {
        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
        synchronizations.forEach(TransactionSynchronization::beforeCompletion);
        synchronizations.forEach(synchronization ->
                synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
    }

    private int occurrences(String value, String target) {
        return (value.length() - value.replace(target, "").length()) / target.length();
    }
}
