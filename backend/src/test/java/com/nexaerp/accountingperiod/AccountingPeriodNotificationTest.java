package com.nexaerp.accountingperiod;

import com.nexaerp.accountingperiod.dto.PeriodCloseChecklistResponseDto;
import com.nexaerp.audit.AuditLogService;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.fiscalyear.FiscalYear;
import com.nexaerp.fiscalyear.FiscalYearRepository;
import com.nexaerp.notification.NotificationModule;
import com.nexaerp.notification.NotificationPriority;
import com.nexaerp.notification.NotificationService;
import com.nexaerp.notification.NotificationType;
import com.nexaerp.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountingPeriodNotificationTest {

    @Mock private AccountingPeriodRepository accountingPeriodRepository;
    @Mock private FiscalYearRepository fiscalYearRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private UserRepository userRepository;
    @Mock private PeriodCloseValidationService periodCloseValidationService;
    @Mock private NotificationService notificationService;
    @InjectMocks private AccountingPeriodServiceImpl service;

    @Test
    void closeCreatesOneNotificationForRealTransition() {
        AccountingPeriod period = period(AccountingPeriodStatus.OPEN);
        when(accountingPeriodRepository.findByIdAndDeletedAtIsNull(5L))
                .thenReturn(Optional.of(period));
        when(periodCloseValidationService.runChecklist(5L))
                .thenReturn(PeriodCloseChecklistResponseDto.builder()
                        .periodId(5L).periodName("July 2026").allPassed(true).checks(List.of()).build());
        when(accountingPeriodRepository.save(any(AccountingPeriod.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.close(5L, null);

        verify(notificationService).scheduleUniqueForCurrentUserAfterCommit(
                NotificationType.ACCOUNTING_PERIOD_CLOSED,
                NotificationPriority.HIGH,
                NotificationModule.ACCOUNTING_PERIOD,
                "Accounting period closed",
                "Accounting period July 2026 (2026-07-01 to 2026-07-31) was closed.",
                "/accounting-periods",
                "ACCOUNTING_PERIOD",
                5L
        );
    }

    @Test
    void lockCreatesOneNotificationForRealTransition() {
        AccountingPeriod period = period(AccountingPeriodStatus.CLOSED);
        when(accountingPeriodRepository.findByIdAndDeletedAtIsNull(5L))
                .thenReturn(Optional.of(period));
        when(accountingPeriodRepository.save(any(AccountingPeriod.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.lock(5L, null);

        verify(notificationService).scheduleUniqueForCurrentUserAfterCommit(
                NotificationType.ACCOUNTING_PERIOD_LOCKED,
                NotificationPriority.CRITICAL,
                NotificationModule.ACCOUNTING_PERIOD,
                "Accounting period locked",
                "Accounting period July 2026 (2026-07-01 to 2026-07-31) was locked.",
                "/accounting-periods",
                "ACCOUNTING_PERIOD",
                5L
        );
    }

    @Test
    void rejectedTransitionCreatesNoNotification() {
        AccountingPeriod period = period(AccountingPeriodStatus.CLOSED);
        when(accountingPeriodRepository.findByIdAndDeletedAtIsNull(5L))
                .thenReturn(Optional.of(period));

        assertThrows(BusinessRuleException.class, () -> service.close(5L, null));

        verify(notificationService, never()).scheduleUniqueForCurrentUserAfterCommit(
                any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    private AccountingPeriod period(AccountingPeriodStatus status) {
        FiscalYear fiscalYear = FiscalYear.builder().id(1L).name("FY 2026").build();
        return AccountingPeriod.builder()
                .id(5L)
                .fiscalYear(fiscalYear)
                .name("July 2026")
                .periodNumber(7)
                .startDate(LocalDate.of(2026, 7, 1))
                .endDate(LocalDate.of(2026, 7, 31))
                .status(status)
                .build();
    }
}
