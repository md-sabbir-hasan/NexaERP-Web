package com.nexaerp.report;

import com.nexaerp.account.*;
import com.nexaerp.accountingperiod.*;
import com.nexaerp.budget.*;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.fiscalyear.FiscalYear;
import com.nexaerp.fiscalyear.FiscalYearStatus;
import com.nexaerp.report.dto.BudgetVsActualResponseDto;
import com.nexaerp.settings.SystemSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BudgetVsActualReportServiceImplTest {
    BudgetRepository budgets = mock(BudgetRepository.class);
    BudgetLineRepository lines = mock(BudgetLineRepository.class);
    BudgetPeriodAllocationRepository allocations = mock(BudgetPeriodAllocationRepository.class);
    AccountingPeriodRepository periods = mock(AccountingPeriodRepository.class);
    BudgetActualService actuals = mock(BudgetActualService.class);
    SystemSettingsService settings = mock(SystemSettingsService.class);
    BudgetVsActualReportServiceImpl service;
    Budget budget;
    Account revenue;
    Account expense;
    List<AccountingPeriod> yearPeriods;
    List<BudgetLine> budgetLines;

    @BeforeEach void setUp() {
        service = new BudgetVsActualReportServiceImpl(budgets, lines, allocations, periods, actuals, settings);
        FiscalYear year = FiscalYear.builder().id(10L).name("FY 2026")
                .startDate(LocalDate.parse("2026-01-01")).endDate(LocalDate.parse("2026-03-31"))
                .status(FiscalYearStatus.ACTIVE).build();
        budget = Budget.builder().id(1L).budgetNumber("BUD-0001").name("Operating Budget")
                .fiscalYear(year).status(BudgetStatus.ACTIVE).build();
        revenue = account(101L, "4000", AccountType.REVENUE);
        expense = account(201L, "5000", AccountType.EXPENSE);
        budgetLines = List.of(line(11L, revenue), line(12L, expense));
        yearPeriods = List.of(period(21L, 1, "2026-01-01", "2026-01-31"),
                period(22L, 2, "2026-02-01", "2026-02-28"),
                period(23L, 3, "2026-03-01", "2026-03-31"));
        when(budgets.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(budget));
        when(lines.findByBudgetId(1L)).thenReturn(budgetLines);
        when(periods.findByFiscalYearIdAndDeletedAtIsNullOrderByPeriodNumberAsc(10L)).thenReturn(yearPeriods);
        when(actuals.getActualByAccounts(anyList(), any(), any())).thenReturn(Map.of(101L,
                new BigDecimal("120"), 201L, new BigDecimal("80")));
        when(settings.getValue(any())).thenReturn("BDT");
        mockAllocations("100", "100");
    }

    @Test void fullYearCalculatesRevenueAndExpenseTotalsAndStatuses() {
        BudgetVsActualResponseDto result = service.generate(1L, null, null, null);
        assertThat(result.getFromDate()).isEqualTo("2026-01-01");
        assertThat(result.getToDate()).isEqualTo("2026-03-31");
        assertThat(result.getRevenueLines()).singleElement().satisfies(line -> {
            assertThat(line.getVarianceAmount()).isEqualByComparingTo("20");
            assertThat(line.getRemainingAmount()).isEqualByComparingTo("-20");
            assertThat(line.getVarianceStatus()).isEqualTo(VarianceStatus.FAVORABLE);
        });
        assertThat(result.getExpenseLines()).singleElement().satisfies(line -> {
            assertThat(line.getVarianceAmount()).isEqualByComparingTo("20");
            assertThat(line.getRemainingAmount()).isEqualByComparingTo("20");
            assertThat(line.getVarianceStatus()).isEqualTo(VarianceStatus.FAVORABLE);
        });
        assertThat(result.getTotalRevenueBudget()).isEqualByComparingTo("100");
        assertThat(result.getTotalExpenseActual()).isEqualByComparingTo("80");
    }

    @Test void unfavorableAndOnTargetStatusesAreAccountTypeAware() {
        when(actuals.getActualByAccounts(anyList(), any(), any())).thenReturn(Map.of(101L,
                new BigDecimal("90"), 201L, new BigDecimal("120")));
        BudgetVsActualResponseDto unfavorable = service.generate(1L, null, null, null);
        assertThat(unfavorable.getRevenueLines().get(0).getVarianceStatus()).isEqualTo(VarianceStatus.UNFAVORABLE);
        assertThat(unfavorable.getExpenseLines().get(0).getVarianceStatus()).isEqualTo(VarianceStatus.UNFAVORABLE);
        when(actuals.getActualByAccounts(anyList(), any(), any())).thenReturn(Map.of(101L,
                new BigDecimal("100"), 201L, new BigDecimal("100")));
        BudgetVsActualResponseDto exact = service.generate(1L, null, null, null);
        assertThat(exact.getRevenueLines().get(0).getVarianceStatus()).isEqualTo(VarianceStatus.ON_TARGET);
        assertThat(exact.getExpenseLines().get(0).getVarianceStatus()).isEqualTo(VarianceStatus.ON_TARGET);
    }

    @Test void zeroBudgetPercentagesAreNull() {
        mockAllocations("0", "0");
        BudgetVsActualResponseDto result = service.generate(1L, null, null, null);
        assertThat(result.getRevenueLines().get(0).getVariancePercent()).isNull();
        assertThat(result.getRevenueLines().get(0).getUtilizationPercent()).isNull();
        assertThat(result.getExpenseUtilizationPercent()).isNull();
    }

    @Test void singleAndContiguousPeriodRangesDeriveInclusiveDates() {
        assertThat(service.generate(1L, 22L, 22L, null).getFromDate()).isEqualTo("2026-02-01");
        BudgetVsActualResponseDto result = service.generate(1L, 21L, 22L, null);
        assertThat(result.getToDate()).isEqualTo("2026-02-28");
        verify(actuals).getActualByAccounts(anyList(), eq(LocalDate.parse("2026-01-01")),
                eq(LocalDate.parse("2026-02-28")));
    }

    @Test void invalidReversedIncompleteAndForeignPeriodRangesAreRejected() {
        assertThatThrownBy(() -> service.generate(1L, 23L, 21L, null)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.generate(1L, 21L, null, null)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.generate(1L, 999L, 999L, null)).isInstanceOf(BusinessRuleException.class);
        yearPeriods.get(2).setStartDate(LocalDate.parse("2026-03-02"));
        assertThatThrownBy(() -> service.generate(1L, 21L, 23L, null)).isInstanceOf(BusinessRuleException.class);
    }

    @Test void activeAndClosedAreReportableButDraftAndCancelledAreRejected() {
        assertThatCode(() -> service.generate(1L, null, null, null)).doesNotThrowAnyException();
        budget.setStatus(BudgetStatus.CLOSED);
        assertThatCode(() -> service.generate(1L, null, null, null)).doesNotThrowAnyException();
        budget.setStatus(BudgetStatus.DRAFT);
        assertThatThrownBy(() -> service.generate(1L, null, null, null)).isInstanceOf(BusinessRuleException.class);
        assertThatCode(() -> service.generateLegacy(1L, null, null, null)).doesNotThrowAnyException();
        budget.setStatus(BudgetStatus.CANCELLED);
        assertThatThrownBy(() -> service.generateLegacy(1L, null, null, null)).isInstanceOf(BusinessRuleException.class);
    }

    @Test void accountTypeFilterAndEmptyBudgetAreHandled() {
        BudgetVsActualResponseDto filtered = service.generate(1L, null, null, AccountType.REVENUE);
        assertThat(filtered.getRevenueLines()).hasSize(1);
        assertThat(filtered.getExpenseLines()).isEmpty();
        when(lines.findByBudgetId(1L)).thenReturn(List.of());
        BudgetVsActualResponseDto empty = service.generate(1L, null, null, null);
        assertThat(empty.getRevenueLines()).isEmpty();
        assertThat(empty.getTotalExpenseBudget()).isEqualByComparingTo("0");
    }

    @Test void legacyAndCanonicalUseEquivalentCalculations() {
        BudgetVsActualResponseDto canonical = service.generate(1L, 21L, 23L, null);
        BudgetVsActualResponseDto legacy = service.generateLegacy(1L, null, null, null);
        assertThat(legacy.getTotalRevenueActual()).isEqualByComparingTo(canonical.getTotalRevenueActual());
        assertThat(legacy.getTotalExpenseVariance()).isEqualByComparingTo(canonical.getTotalExpenseVariance());
    }

    @Test void equalAllocationsAreSummedAcrossTheSelectedPeriods() {
        budgetLines.get(0).setAllocationMethod(BudgetAllocationMethod.EQUAL);
        when(allocations.findByBudgetLineIdInAndAccountingPeriodIdIn(anyList(), anyList())).thenReturn(List.of(
                allocation(budgetLines.get(0), yearPeriods.get(0), "33.33"),
                allocation(budgetLines.get(0), yearPeriods.get(1), "33.33"),
                allocation(budgetLines.get(0), yearPeriods.get(2), "33.34")));
        BudgetVsActualResponseDto result = service.generate(1L, null, null, AccountType.REVENUE);
        assertThat(result.getRevenueLines().get(0).getBudgetAmount()).isEqualByComparingTo("100");
    }

    @Test void manualAllocationsUseOnlyTheSelectedPeriodRange() {
        budgetLines.get(1).setAllocationMethod(BudgetAllocationMethod.MANUAL);
        when(allocations.findByBudgetLineIdInAndAccountingPeriodIdIn(anyList(), eq(List.of(22L))))
                .thenReturn(List.of(allocation(budgetLines.get(1), yearPeriods.get(1), "45")));
        BudgetVsActualResponseDto result = service.generate(1L, 22L, 22L, AccountType.EXPENSE);
        assertThat(result.getExpenseLines().get(0).getBudgetAmount()).isEqualByComparingTo("45");
    }

    private void mockAllocations(String revenueAmount, String expenseAmount) {
        List<BudgetPeriodAllocation> values = List.of(allocation(budgetLines.get(0), yearPeriods.get(0), revenueAmount),
                allocation(budgetLines.get(1), yearPeriods.get(0), expenseAmount));
        when(allocations.findByBudgetLineIdInAndAccountingPeriodIdIn(anyList(), anyList())).thenReturn(values);
    }

    private BudgetPeriodAllocation allocation(BudgetLine line, AccountingPeriod period, String amount) {
        return BudgetPeriodAllocation.builder().budgetLine(line).accountingPeriod(period)
                .budgetAmount(new BigDecimal(amount)).build();
    }

    private BudgetLine line(Long id, Account account) {
        return BudgetLine.builder().id(id).budget(budget).account(account).annualAmount(BigDecimal.ZERO)
                .allocationMethod(BudgetAllocationMethod.MANUAL).build();
    }

    private AccountingPeriod period(Long id, int number, String start, String end) {
        return AccountingPeriod.builder().id(id).fiscalYear(budget.getFiscalYear()).name("P" + number)
                .periodNumber(number).startDate(LocalDate.parse(start)).endDate(LocalDate.parse(end)).build();
    }

    private Account account(Long id, String code, AccountType type) {
        return new Account(id, code, code, null, type, true, false, false,
                BigDecimal.ZERO, null, List.of());
    }
}
