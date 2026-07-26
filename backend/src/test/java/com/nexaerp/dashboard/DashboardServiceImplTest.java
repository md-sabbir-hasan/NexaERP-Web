package com.nexaerp.dashboard;

import com.nexaerp.account.AccountRepository;
import com.nexaerp.account.AccountType;
import com.nexaerp.accountingperiod.AccountingPeriodRepository;
import com.nexaerp.audit.AuditLogRepository;
import com.nexaerp.budget.BudgetRepository;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.expense.ExpenseRepository;
import com.nexaerp.fiscalyear.FiscalYearRepository;
import com.nexaerp.invoice.InvoiceRepository;
import com.nexaerp.journal.JournalEntryRepository;
import com.nexaerp.journal.JournalStatus;
import com.nexaerp.permission.PermissionRepository;
import com.nexaerp.recurringexpense.RecurringExpenseTemplateRepository;
import com.nexaerp.report.BudgetVsActualReportService;
import com.nexaerp.report.CashFlowStatementService;
import com.nexaerp.report.dto.CashFlowStatementResponseDto;
import com.nexaerp.role.RoleRepository;
import com.nexaerp.user.UserRepository;
import com.nexaerp.vendorbill.VendorBillRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {
    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock PermissionRepository permissionRepository;
    @Mock AccountRepository accountRepository;
    @Mock JournalEntryRepository journalEntryRepository;
    @Mock AuditLogRepository auditLogRepository;
    @Mock InvoiceRepository invoiceRepository;
    @Mock VendorBillRepository vendorBillRepository;
    @Mock DashboardFinanceRepository dashboardFinanceRepository;
    @Mock BudgetRepository budgetRepository;
    @Mock FiscalYearRepository fiscalYearRepository;
    @Mock AccountingPeriodRepository accountingPeriodRepository;
    @Mock ExpenseRepository expenseRepository;
    @Mock RecurringExpenseTemplateRepository recurringExpenseTemplateRepository;
    @Mock CashFlowStatementService cashFlowStatementService;
    @Mock BudgetVsActualReportService budgetVsActualReportService;
    @InjectMocks DashboardServiceImpl service;

    @AfterEach void clearSecurity() { SecurityContextHolder.clearContext(); }

    @Test
    void unauthorizedSectionsAreAbsentAndRepositoriesAreSkipped() {
        authenticate();
        var result = service.getSummary();
        assertThat(result.getBusiness()).isNull();
        assertThat(result.getBudget()).isNull();
        assertThat(result.getRecentActivities()).isNull();
        verifyNoInteractions(invoiceRepository, vendorBillRepository, dashboardFinanceRepository,
                cashFlowStatementService, budgetVsActualReportService, auditLogRepository);
    }

    @Test
    void cashUsesCashFlowLedgerClosingBalance() {
        authenticate("VIEW_BANKING");
        when(cashFlowStatementService.generate(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(CashFlowStatementResponseDto.builder().currencyCode("BDT")
                        .ledgerClosingCashBalance(new BigDecimal("1250.75")).build());
        var business = service.getSummary().getBusiness();
        assertThat(business.getCashPosition()).isEqualByComparingTo("1250.75");
        assertThat(business.getCashConfigured()).isTrue();
        verifyNoInteractions(invoiceRepository, vendorBillRepository, dashboardFinanceRepository);
    }

    @Test
    void missingCashConfigurationProducesUnavailableCashWithoutBlockingDashboard() {
        authenticate("VIEW_BANKING");
        when(cashFlowStatementService.generate(any(LocalDate.class), any(LocalDate.class)))
                .thenThrow(new BusinessRuleException("No cash or cash-equivalent accounts are configured"));

        var business = service.getSummary().getBusiness();

        assertThat(business.getCashPosition()).isNull();
        assertThat(business.getCashConfigured()).isFalse();
    }

    @Test
    void noJournalTrendRowsProducesSixZeroMonths() {
        authenticate("VIEW_REPORT");
        when(dashboardFinanceRepository.aggregateMonthlyNaturalBalances(
                anyCollection(), any(LocalDate.class), any(LocalDate.class), anyCollection()))
                .thenReturn(List.of());

        var business = service.getSummary().getBusiness();

        assertThat(business.getRevenueTrend()).hasSize(6)
                .allSatisfy(point -> assertThat(point.getAmount()).isEqualByComparingTo(BigDecimal.ZERO));
        assertThat(business.getExpenseTrend()).hasSize(6)
                .allSatisfy(point -> assertThat(point.getAmount()).isEqualByComparingTo(BigDecimal.ZERO));
    }

    @Test
    void trendUsesNaturalBalancesPreservesNegativeValuesAndMapsMissingMonths() {
        authenticate("VIEW_REPORT");
        when(dashboardFinanceRepository.aggregateMonthlyNaturalBalances(
                anyCollection(), any(LocalDate.class), any(LocalDate.class), anyCollection()))
                .thenReturn(List.of(
                        new Object[]{LocalDate.now().getYear(), LocalDate.now().getMonthValue(), AccountType.REVENUE,
                                new BigDecimal("150"), new BigDecimal("100")},
                        new Object[]{LocalDate.now().getYear(), LocalDate.now().getMonthValue(), AccountType.EXPENSE,
                                new BigDecimal("20"), new BigDecimal("45")}));
        var business = service.getSummary().getBusiness();
        assertThat(business.getRevenueTrend()).hasSize(6);
        assertThat(business.getExpenseTrend()).hasSize(6);
        assertThat(business.getRevenueTrend().get(5).getAmount()).isEqualByComparingTo("-50");
        assertThat(business.getExpenseTrend().get(5).getAmount()).isEqualByComparingTo("-25");
        assertThat(business.getRevenueTrend().get(0).getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void vendorBillBalanceDoesNotQueryExpenseDue() {
        authenticate("VIEW_VENDOR_BILL");
        when(vendorBillRepository.sumOutstandingPayable()).thenReturn(new BigDecimal("400"));
        when(vendorBillRepository.sumOverdueAmount(any())).thenReturn(BigDecimal.ZERO);
        var business = service.getSummary().getBusiness();
        assertThat(business.getAccountsPayable()).isEqualByComparingTo("400");
        verify(expenseRepository, never()).sumOutstandingDue();
    }

    @Test
    void noActiveFiscalYearProducesUnavailableBudgetWithoutBlockingDashboard() {
        authenticate("VIEW_BUDGET_REPORT");
        when(fiscalYearRepository.findFirstByStatusAndDeletedAtIsNull(any())).thenReturn(Optional.empty());
        var budget = service.getSummary().getBudget();
        assertThat(budget.isHasActiveBudget()).isFalse();
        assertThat(budget.getUnavailableReason()).isEqualTo("No active fiscal year");
        verifyNoInteractions(budgetVsActualReportService);
    }

    private void authenticate(String... permissions) {
        var authorities = java.util.Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("dashboard-test", "n/a", authorities));
    }
}
