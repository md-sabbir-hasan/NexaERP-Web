package com.nexaerp.dashboard;

import com.nexaerp.account.AccountRepository;
import com.nexaerp.account.AccountType;
import com.nexaerp.audit.AuditLog;
import com.nexaerp.audit.AuditLogRepository;
import com.nexaerp.banking.repository.BankAccountRepository;
import com.nexaerp.budget.*;
import com.nexaerp.dashboard.dto.*;
import com.nexaerp.expense.ExpenseRepository;
import com.nexaerp.expense.ExpenseStatus;
import com.nexaerp.fiscalyear.FiscalYear;
import com.nexaerp.fiscalyear.FiscalYearRepository;
import com.nexaerp.fiscalyear.FiscalYearStatus;
import com.nexaerp.invoice.InvoiceRepository;
import com.nexaerp.journal.JournalEntryRepository;
import com.nexaerp.journal.JournalStatus;
import com.nexaerp.permission.PermissionRepository;
import com.nexaerp.recurringexpense.RecurringExpenseStatus;
import com.nexaerp.recurringexpense.RecurringExpenseTemplateRepository;
import com.nexaerp.role.RoleRepository;
import com.nexaerp.user.UserRepository;
import com.nexaerp.user.UserStatus;
import com.nexaerp.vendorbill.VendorBillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.nexaerp.dashboard.dto.HealthSummaryDto;

import java.math.RoundingMode;
import java.time.ZoneId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService{
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final AuditLogRepository auditLogRepository;
    private final InvoiceRepository invoiceRepository;
    private final VendorBillRepository vendorBillRepository;
    private final BankAccountRepository bankAccountRepository;

    private final DashboardFinanceRepository dashboardFinanceRepository;
    private final BudgetRepository budgetRepository;
    private final BudgetLineRepository budgetLineRepository;
    private final BudgetActualService budgetActualService;
    private final FiscalYearRepository fiscalYearRepository;
    private final ExpenseRepository expenseRepository;
    private final RecurringExpenseTemplateRepository recurringExpenseTemplateRepository;

    @Value("${app.version:1.0.0}")
    private String applicationVersion;

    @Value("${spring.profiles.active:default}")
    private String environment;

    @Override
    public DashboardSummaryDto getSummary() {
        List<RecentActivityDto> recentActivities = auditLogRepository
                .findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(this::toActivity)
                .toList();


        return DashboardSummaryDto.builder()
                .users(buildUserSummary())
                .security(buildSecuritySummary())
                .finance(buildFinanceSummary())
                .business(buildBusinessSummary())
                .system(buildSystemSummary())
                .recentActivities(recentActivities)
                .health(buildHealthSummary())
                .budget(buildBudgetSummary())
                .expense(buildExpenseSummary())
                .build();
    }

    private UserSummaryDto buildUserSummary() {
        return UserSummaryDto.builder()
                .total(userRepository.count())
                .active(userRepository.countByStatus(UserStatus.ACTIVE))
                .pending(userRepository.countByStatus(UserStatus.PENDING))
                .inactive(userRepository.countByStatus(UserStatus.INACTIVE))
                .locked(userRepository.countByStatus(UserStatus.LOCKED))
                .build();
    }

    private SecuritySummaryDto buildSecuritySummary() {
        return SecuritySummaryDto.builder()
                .totalRoles(roleRepository.count())
                .totalPermissions(permissionRepository.count())
                .build();
    }

    private FinanceSummaryDto buildFinanceSummary() {
        return FinanceSummaryDto.builder()
                .totalAccounts(accountRepository.count())
                .totalJournalEntries(journalEntryRepository.count())
                .postedJournalEntries(journalEntryRepository.countByStatus(JournalStatus.POSTED))
                .draftJournalEntries(journalEntryRepository.countByStatus(JournalStatus.DRAFT))
                .reversedJournalEntries(journalEntryRepository.countByStatus(JournalStatus.REVERSED))
                .build();
    }

    private SystemSummaryDto buildSystemSummary() {
        return SystemSummaryDto.builder()
                .applicationVersion(applicationVersion)
                .serverTime(LocalDateTime.now())
                .serverTimezone(ZoneId.systemDefault().toString())
                .environment(environment)
                .javaVersion(System.getProperty("java.version"))
                .build();
    }

    private BusinessSummaryDto buildBusinessSummary() {
        LocalDate today = LocalDate.now();

        BigDecimal vendorBillPayable = vendorBillRepository.sumOutstandingPayable();
        BigDecimal expensePayable = expenseRepository.sumOutstandingDue();

        return BusinessSummaryDto.builder()
                .cashPosition(bankAccountRepository.sumActiveBalances())
                .accountsReceivable(invoiceRepository.sumOutstandingReceivable())
                .overdueInvoiceCount(invoiceRepository.countOverdue(today))
                .overdueInvoiceAmount(invoiceRepository.sumOverdueAmount(today))
                .accountsPayable(vendorBillPayable.add(expensePayable))
                .overdueBillCount(vendorBillRepository.countOverdue(today))
                .overdueBillAmount(vendorBillRepository.sumOverdueAmount(today))
                .revenueTrend(buildMonthlyTrend(true))
                .expenseTrend(buildMonthlyTrend(false))
                .build();
    }

    // Builds a 6-month trend (oldest -> newest, including current month)
    // Now sourced directly from POSTED journal lines — covers Invoice, VendorBill,
    // Expense, FixedAsset depreciation, and manual journal entries all at once.
    private List<MonthlyTrendDto> buildMonthlyTrend(boolean revenue) {
        List<MonthlyTrendDto> trend = new ArrayList<>();
        DateTimeFormatter labelFormat = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

        YearMonth current = YearMonth.now();

        for (int i = 5; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            LocalDate from = month.atDay(1);
            LocalDate to = month.atEndOfMonth();

            BigDecimal amount = revenue
                    ? dashboardFinanceRepository
                    .sumNetDebitBetween(AccountType.REVENUE, from, to, JournalStatus.POSTED)
                    .negate()
                    : dashboardFinanceRepository
                    .sumNetDebitBetween(AccountType.EXPENSE, from, to, JournalStatus.POSTED);

            trend.add(MonthlyTrendDto.builder()
                    .month(month.format(labelFormat))
                    .amount(amount)
                    .build());
        }

        return trend;
    }

    private RecentActivityDto toActivity(AuditLog audit) {
        return RecentActivityDto.builder()
                .action(audit.getAction().name())
                .entityName(audit.getEntityName())
                .entityId(audit.getEntityId())
                .userName(audit.getUserName())
                .createdAt(audit.getCreatedAt())
                .description(
                        audit.getAction() + " " + audit.getEntityName()
                )
                .build();
    }

    private HealthSummaryDto buildHealthSummary() {
        return HealthSummaryDto.builder()
                .application("UP")
                .database("UP")
                .mail("UP")
                .build();
    }

    private BudgetDashboardDto buildBudgetSummary() {
        LocalDate today = LocalDate.now();

        FiscalYear activeFiscalYear = fiscalYearRepository.findAll().stream()
                .filter(fy -> fy.getStatus() == FiscalYearStatus.ACTIVE)
                .findFirst()
                .orElse(null);

        if (activeFiscalYear == null) {
            return BudgetDashboardDto.builder().hasActiveBudget(false).build();
        }

        Budget activeBudget = budgetRepository
                .findByFiscalYearIdAndStatusAndDeletedAtIsNull(activeFiscalYear.getId(), BudgetStatus.ACTIVE)
                .orElse(null);

        if (activeBudget == null) {
            return BudgetDashboardDto.builder().hasActiveBudget(false).build();
        }

        BigDecimal expenseActualYtd = dashboardFinanceRepository.sumNetDebitBetween(
                AccountType.EXPENSE, activeFiscalYear.getStartDate(), today, JournalStatus.POSTED);

        BigDecimal revenueActualYtd = dashboardFinanceRepository.sumNetDebitBetween(
                AccountType.REVENUE, activeFiscalYear.getStartDate(), today, JournalStatus.POSTED).negate();

        BigDecimal expenseUtilization = activeBudget.getTotalExpenseBudget().compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : expenseActualYtd.divide(activeBudget.getTotalExpenseBudget(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        BigDecimal revenueAchievement = activeBudget.getTotalRevenueBudget().compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : revenueActualYtd.divide(activeBudget.getTotalRevenueBudget(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        List<BudgetLine> expenseLines = budgetLineRepository.findByBudgetId(activeBudget.getId()).stream()
                .filter(l -> l.getAccount().getType() == AccountType.EXPENSE)
                .collect(Collectors.toList());

        List<com.nexaerp.account.Account> accounts = expenseLines.stream()
                .map(BudgetLine::getAccount).collect(Collectors.toList());

        Map<Long, BigDecimal> actualsByAccount = budgetActualService.getActualByAccounts(
                accounts, activeFiscalYear.getStartDate(), today);

        List<BudgetTopAccountDto> topAccounts = expenseLines.stream()
                .map(line -> {
                    BigDecimal actual = actualsByAccount.getOrDefault(line.getAccount().getId(), BigDecimal.ZERO);
                    BigDecimal util = line.getAnnualAmount().compareTo(BigDecimal.ZERO) == 0
                            ? BigDecimal.ZERO
                            : actual.divide(line.getAnnualAmount(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
                    return BudgetTopAccountDto.builder()
                            .accountName(line.getAccount().getName())
                            .budgetAmount(line.getAnnualAmount())
                            .actualAmount(actual)
                            .utilizationPercent(util)
                            .build();
                })
                .sorted((a, b) -> b.getUtilizationPercent().compareTo(a.getUtilizationPercent()))
                .limit(3)
                .collect(Collectors.toList());

        return BudgetDashboardDto.builder()
                .hasActiveBudget(true)
                .activeBudgetId(activeBudget.getId())
                .activeBudgetName(activeBudget.getName())
                .totalExpenseBudget(activeBudget.getTotalExpenseBudget())
                .totalExpenseActualYtd(expenseActualYtd)
                .expenseUtilizationPercent(expenseUtilization)
                .totalRevenueBudget(activeBudget.getTotalRevenueBudget())
                .totalRevenueActualYtd(revenueActualYtd)
                .revenueAchievementPercent(revenueAchievement)
                .topAccounts(topAccounts)
                .build();
    }

    private ExpenseDashboardDto buildExpenseSummary() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        return ExpenseDashboardDto.builder()
                .draftCount(expenseRepository.countByStatus(ExpenseStatus.DRAFT))
                .draftTotalAmount(expenseRepository.sumAmountByStatus(ExpenseStatus.DRAFT))
                .postedThisMonthTotal(expenseRepository.sumAmountByStatusAndDateBetween(
                        ExpenseStatus.POSTED, monthStart, today))
                .recurringActiveCount(recurringExpenseTemplateRepository.countByStatus(RecurringExpenseStatus.ACTIVE))
                .recurringDueSoonCount(recurringExpenseTemplateRepository
                        .countByStatusAndNextRunDateLessThanEqualAndDeletedAtIsNull(
                                RecurringExpenseStatus.ACTIVE, today.plusDays(7)))
                .build();
    }
}