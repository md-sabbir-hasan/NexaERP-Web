package com.nexaerp.dashboard;

import com.nexaerp.account.AccountRepository;
import com.nexaerp.account.AccountType;
import com.nexaerp.accountingperiod.AccountingPeriod;
import com.nexaerp.accountingperiod.AccountingPeriodRepository;
import com.nexaerp.audit.AuditLog;
import com.nexaerp.audit.AuditLogRepository;
import com.nexaerp.budget.Budget;
import com.nexaerp.budget.BudgetRepository;
import com.nexaerp.budget.BudgetStatus;
import com.nexaerp.common.exception.BusinessRuleException;
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
import com.nexaerp.report.BudgetVsActualReportService;
import com.nexaerp.report.CashFlowStatementService;
import com.nexaerp.report.dto.BudgetVsActualLineDto;
import com.nexaerp.report.dto.BudgetVsActualResponseDto;
import com.nexaerp.role.RoleRepository;
import com.nexaerp.user.UserRepository;
import com.nexaerp.user.UserStatus;
import com.nexaerp.vendorbill.VendorBillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    private static final Set<JournalStatus> LEDGER_STATUSES = Set.of(JournalStatus.POSTED, JournalStatus.REVERSED);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final AuditLogRepository auditLogRepository;
    private final InvoiceRepository invoiceRepository;
    private final VendorBillRepository vendorBillRepository;
    private final DashboardFinanceRepository dashboardFinanceRepository;
    private final BudgetRepository budgetRepository;
    private final FiscalYearRepository fiscalYearRepository;
    private final AccountingPeriodRepository accountingPeriodRepository;
    private final ExpenseRepository expenseRepository;
    private final RecurringExpenseTemplateRepository recurringExpenseTemplateRepository;
    private final CashFlowStatementService cashFlowStatementService;
    private final BudgetVsActualReportService budgetVsActualReportService;

    @Value("${app.version:1.0.0}") private String applicationVersion;
    @Value("${spring.profiles.active:default}") private String environment;

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryDto getSummary() {
        Set<String> permissions = currentPermissions();
        return DashboardSummaryDto.builder()
                .users(has(permissions, "MANAGE_USERS") ? buildUserSummary() : null)
                .security(buildSecuritySummary(permissions))
                .finance(buildFinanceSummary(permissions))
                .business(buildBusinessSummary(permissions))
                .system(has(permissions, "MANAGE_SETTINGS") ? buildSystemSummary() : null)
                .recentActivities(has(permissions, "VIEW_AUDIT_LOGS") ? buildRecentActivities() : null)
                .budget(has(permissions, "VIEW_BUDGET_REPORT") ? buildBudgetSummary() : null)
                .expense(has(permissions, "VIEW_EXPENSE") ? buildExpenseSummary(permissions) : null)
                .build();
    }

    private Set<String> currentPermissions() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) return Set.of();
        Set<String> permissions = new HashSet<>();
        authentication.getAuthorities().forEach(authority -> permissions.add(authority.getAuthority()));
        return permissions;
    }

    private boolean has(Set<String> permissions, String permission) { return permissions.contains(permission); }

    private UserSummaryDto buildUserSummary() {
        return UserSummaryDto.builder().total(userRepository.count()).active(userRepository.countByStatus(UserStatus.ACTIVE))
                .pending(userRepository.countByStatus(UserStatus.PENDING)).inactive(userRepository.countByStatus(UserStatus.INACTIVE))
                .locked(userRepository.countByStatus(UserStatus.LOCKED)).build();
    }

    private SecuritySummaryDto buildSecuritySummary(Set<String> permissions) {
        boolean roles = has(permissions, "MANAGE_ROLES");
        boolean definitions = has(permissions, "MANAGE_PERMISSIONS");
        if (!roles && !definitions) return null;
        return SecuritySummaryDto.builder().totalRoles(roles ? roleRepository.count() : null)
                .totalPermissions(definitions ? permissionRepository.count() : null).build();
    }

    private FinanceSummaryDto buildFinanceSummary(Set<String> permissions) {
        boolean accounts = has(permissions, "VIEW_ACCOUNTS");
        boolean journals = has(permissions, "VIEW_JOURNAL");
        if (!accounts && !journals) return null;
        return FinanceSummaryDto.builder().totalAccounts(accounts ? accountRepository.count() : null)
                .totalJournalEntries(journals ? journalEntryRepository.count() : null)
                .postedJournalEntries(journals ? journalEntryRepository.countByStatus(JournalStatus.POSTED) : null)
                .draftJournalEntries(journals ? journalEntryRepository.countByStatus(JournalStatus.DRAFT) : null)
                .reversedJournalEntries(journals ? journalEntryRepository.countByStatus(JournalStatus.REVERSED) : null).build();
    }

    private BusinessSummaryDto buildBusinessSummary(Set<String> permissions) {
        boolean banking = has(permissions, "VIEW_BANKING");
        boolean invoices = has(permissions, "VIEW_INVOICE");
        boolean bills = has(permissions, "VIEW_VENDOR_BILL");
        boolean reports = has(permissions, "VIEW_REPORT");
        if (!banking && !invoices && !bills && !reports) return null;
        LocalDate today = LocalDate.now();
        BusinessSummaryDto.BusinessSummaryDtoBuilder builder = BusinessSummaryDto.builder().asOfDate(today);
        if (banking) {
            try {
                var cashFlow = cashFlowStatementService.generate(today, today);
                builder.cashPosition(cashFlow.getLedgerClosingCashBalance()).cashConfigured(true).currencyCode(cashFlow.getCurrencyCode());
            } catch (BusinessRuleException ex) {
                builder.cashConfigured(false);
            }
        }
        if (invoices) builder.accountsReceivable(invoiceRepository.sumOutstandingReceivable())
                .overdueInvoiceCount(invoiceRepository.countOverdue(today)).overdueInvoiceAmount(invoiceRepository.sumOverdueAmount(today));
        if (bills) builder.accountsPayable(vendorBillRepository.sumOutstandingPayable())
                .overdueBillCount(vendorBillRepository.countOverdue(today)).overdueBillAmount(vendorBillRepository.sumOverdueAmount(today));
        if (reports) {
            LocalDate from = YearMonth.now().minusMonths(5).atDay(1);
            Map<AccountType, Map<YearMonth, BigDecimal>> values = loadTrend(from, today);
            builder.revenueTrend(toTrend(values.get(AccountType.REVENUE))).expenseTrend(toTrend(values.get(AccountType.EXPENSE)))
                    .trendFromDate(from).trendToDate(today);
        }
        return builder.build();
    }

    private Map<AccountType, Map<YearMonth, BigDecimal>> loadTrend(LocalDate from, LocalDate to) {
        Map<AccountType, Map<YearMonth, BigDecimal>> result = new EnumMap<>(AccountType.class);
        dashboardFinanceRepository.aggregateMonthlyNaturalBalances(List.of(AccountType.REVENUE, AccountType.EXPENSE), from, to, LEDGER_STATUSES)
                .forEach(row -> {
                    YearMonth month = YearMonth.of(((Number) row[0]).intValue(), ((Number) row[1]).intValue());
                    AccountType type = (AccountType) row[2];
                    BigDecimal debit = (BigDecimal) row[3];
                    BigDecimal credit = (BigDecimal) row[4];
                    BigDecimal natural = type == AccountType.REVENUE ? credit.subtract(debit) : debit.subtract(credit);
                    result.computeIfAbsent(type, ignored -> new HashMap<>()).put(month, natural);
                });
        return result;
    }

    private List<MonthlyTrendDto> toTrend(Map<YearMonth, BigDecimal> values) {
        Map<YearMonth, BigDecimal> safe = values == null ? Map.of() : values;
        DateTimeFormatter format = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);
        List<MonthlyTrendDto> trend = new ArrayList<>();
        YearMonth current = YearMonth.now();
        for (int i = 5; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            trend.add(MonthlyTrendDto.builder().month(month.format(format)).amount(safe.getOrDefault(month, BigDecimal.ZERO)).build());
        }
        return trend;
    }

    private List<RecentActivityDto> buildRecentActivities() {
        return auditLogRepository.findTop10ByOrderByCreatedAtDesc().stream().map(this::toActivity).toList();
    }

    private RecentActivityDto toActivity(AuditLog audit) {
        return RecentActivityDto.builder().action(audit.getAction().name()).entityName(audit.getEntityName())
                .entityId(audit.getEntityId()).userName(audit.getUserName()).createdAt(audit.getCreatedAt())
                .description(audit.getAction() + " " + audit.getEntityName()).build();
    }

    private SystemSummaryDto buildSystemSummary() {
        return SystemSummaryDto.builder().applicationVersion(applicationVersion).serverTime(LocalDateTime.now())
                .serverTimezone(ZoneId.systemDefault().toString()).environment(environment)
                .javaVersion(System.getProperty("java.version")).build();
    }

    private BudgetDashboardDto buildBudgetSummary() {
        FiscalYear fiscalYear = fiscalYearRepository.findFirstByStatusAndDeletedAtIsNull(FiscalYearStatus.ACTIVE).orElse(null);
        if (fiscalYear == null) return unavailableBudget("No active fiscal year");
        Budget budget = budgetRepository.findByFiscalYearIdAndStatusAndDeletedAtIsNull(fiscalYear.getId(), BudgetStatus.ACTIVE).orElse(null);
        if (budget == null) return unavailableBudget("No active budget");
        List<AccountingPeriod> periods = accountingPeriodRepository.findByFiscalYearIdAndDeletedAtIsNullOrderByPeriodNumberAsc(fiscalYear.getId());
        AccountingPeriod current = periods.stream().filter(p -> !LocalDate.now().isBefore(p.getStartDate())
                && !LocalDate.now().isAfter(p.getEndDate())).findFirst().orElse(null);
        if (periods.isEmpty() || current == null) return unavailableBudget("No budget period available");
        BudgetVsActualResponseDto report = budgetVsActualReportService.generate(budget.getId(), periods.get(0).getId(), current.getId(), null);
        List<BudgetTopAccountDto> top = report.getExpenseLines().stream()
                .sorted(Comparator.comparing(BudgetVsActualLineDto::getUtilizationPercent,
                        Comparator.nullsFirst(Comparator.naturalOrder())).reversed()).limit(3)
                .map(line -> BudgetTopAccountDto.builder().accountId(line.getAccountId()).accountCode(line.getAccountCode())
                        .accountName(line.getAccountName()).budgetAmount(line.getBudgetAmount())
                        .actualAmount(line.getActualAmount()).utilizationPercent(line.getUtilizationPercent()).build()).toList();
        return BudgetDashboardDto.builder().hasActiveBudget(true).activeBudgetId(report.getBudgetId()).activeBudgetName(report.getBudgetName())
                .fromDate(report.getFromDate()).toDate(report.getToDate()).currencyCode(report.getCurrencyCode())
                .totalExpenseBudget(report.getTotalExpenseBudget()).totalExpenseActualYtd(report.getTotalExpenseActual())
                .expenseUtilizationPercent(report.getExpenseUtilizationPercent()).totalRevenueBudget(report.getTotalRevenueBudget())
                .totalRevenueActualYtd(report.getTotalRevenueActual()).revenueAchievementPercent(report.getRevenueAchievementPercent())
                .topAccounts(top).build();
    }

    private BudgetDashboardDto unavailableBudget(String reason) {
        return BudgetDashboardDto.builder().hasActiveBudget(false).unavailableReason(reason).topAccounts(List.of()).build();
    }

    private ExpenseDashboardDto buildExpenseSummary(Set<String> permissions) {
        LocalDate today = LocalDate.now();
        ExpenseDashboardDto.ExpenseDashboardDtoBuilder builder = ExpenseDashboardDto.builder()
                .draftCount(expenseRepository.countByStatus(ExpenseStatus.DRAFT)).draftTotalAmount(expenseRepository.sumAmountByStatus(ExpenseStatus.DRAFT))
                .postedThisMonthTotal(expenseRepository.sumAmountByStatusAndDateBetween(ExpenseStatus.POSTED, today.withDayOfMonth(1), today))
                .outstandingDue(expenseRepository.sumOutstandingDue());
        if (has(permissions, "VIEW_RECURRING_EXPENSE")) {
            builder.recurringActiveCount(recurringExpenseTemplateRepository.countByStatus(RecurringExpenseStatus.ACTIVE))
                    .recurringDueSoonCount(recurringExpenseTemplateRepository.countByStatusAndNextRunDateLessThanEqualAndDeletedAtIsNull(
                            RecurringExpenseStatus.ACTIVE, today.plusDays(7)));
        }
        return builder.build();
    }
}
