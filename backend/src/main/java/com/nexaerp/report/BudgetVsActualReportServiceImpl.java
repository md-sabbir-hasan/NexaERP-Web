package com.nexaerp.report;

import com.nexaerp.account.Account;
import com.nexaerp.account.AccountType;
import com.nexaerp.accountingperiod.AccountingPeriod;
import com.nexaerp.accountingperiod.AccountingPeriodRepository;
import com.nexaerp.budget.*;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.report.dto.BudgetVsActualLineDto;
import com.nexaerp.report.dto.BudgetVsActualResponseDto;
import com.nexaerp.report.dto.BudgetVsActualOptionDto;
import com.nexaerp.report.dto.BudgetVsActualPeriodOptionDto;
import com.nexaerp.settings.SettingKey;
import com.nexaerp.settings.SystemSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetVsActualReportServiceImpl implements BudgetVsActualReportService {
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final BudgetRepository budgetRepository;
    private final BudgetLineRepository budgetLineRepository;
    private final BudgetPeriodAllocationRepository allocationRepository;
    private final AccountingPeriodRepository accountingPeriodRepository;
    private final BudgetActualService budgetActualService;
    private final SystemSettingsService systemSettingsService;

    @Override
    @Transactional(readOnly = true)
    public BudgetVsActualResponseDto generate(
            Long budgetId, Long fromPeriodId, Long toPeriodId, AccountType accountType) {
        Budget budget = getBudget(budgetId);
        if (budget.getStatus() != BudgetStatus.ACTIVE && budget.getStatus() != BudgetStatus.CLOSED) {
            throw new BusinessRuleException("Only ACTIVE or CLOSED budgets can be used for the professional report");
        }
        if (accountType != null && accountType != AccountType.REVENUE && accountType != AccountType.EXPENSE) {
            throw new BusinessRuleException("accountType must be REVENUE or EXPENSE");
        }
        List<AccountingPeriod> periods = selectPeriods(periodsFor(budget), fromPeriodId, toPeriodId);
        return calculate(budget, periods, periods.get(0).getStartDate(),
                periods.get(periods.size() - 1).getEndDate(), accountType);
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetVsActualResponseDto generateLegacy(
            Long budgetId, Long periodId, LocalDate fromDate, LocalDate toDate) {
        Budget budget = getBudget(budgetId);
        if (budget.getStatus() == BudgetStatus.CANCELLED) {
            throw new BusinessRuleException("Cancelled budgets cannot be reported");
        }
        List<AccountingPeriod> allPeriods = periodsFor(budget);
        if (periodId != null) {
            AccountingPeriod period = allPeriods.stream().filter(p -> p.getId().equals(periodId))
                    .findFirst().orElseThrow(() -> new BusinessRuleException(
                            "Accounting period does not belong to the budget fiscal year"));
            return calculate(budget, List.of(period), period.getStartDate(), period.getEndDate(), null);
        }
        if ((fromDate == null) != (toDate == null)) {
            throw new BusinessRuleException("Both fromDate and toDate are required");
        }
        LocalDate rangeFrom = fromDate != null ? fromDate : budget.getFiscalYear().getStartDate();
        LocalDate rangeTo = toDate != null ? toDate : budget.getFiscalYear().getEndDate();
        if (rangeFrom.isAfter(rangeTo)) {
            throw new BusinessRuleException("fromDate must not be after toDate");
        }
        List<AccountingPeriod> selected = allPeriods.stream()
                .filter(p -> !p.getStartDate().isBefore(rangeFrom) && !p.getEndDate().isAfter(rangeTo))
                .toList();
        return calculate(budget, selected, rangeFrom, rangeTo, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetVsActualOptionDto> getOptions() {
        return budgetRepository.findByStatusInAndDeletedAtIsNullOrderByCreatedAtDesc(
                        List.of(BudgetStatus.ACTIVE, BudgetStatus.CLOSED, BudgetStatus.DRAFT)).stream()
                .map(budget -> BudgetVsActualOptionDto.builder()
                        .budgetId(budget.getId()).budgetNumber(budget.getBudgetNumber())
                        .budgetName(budget.getName()).budgetStatus(budget.getStatus())
                        .fiscalYearId(budget.getFiscalYear().getId())
                        .fiscalYearName(budget.getFiscalYear().getName())
                        .periods(periodsFor(budget).stream().map(period ->
                                BudgetVsActualPeriodOptionDto.builder().id(period.getId())
                                        .name(period.getName()).periodNumber(period.getPeriodNumber())
                                        .startDate(period.getStartDate()).endDate(period.getEndDate()).build())
                                .toList()).build()).toList();
    }

    private BudgetVsActualResponseDto calculate(
            Budget budget, List<AccountingPeriod> selectedPeriods, LocalDate fromDate,
            LocalDate toDate, AccountType filter) {
        List<BudgetLine> lines = budgetLineRepository.findByBudgetId(budget.getId()).stream()
                .filter(line -> line.getAccount().getType() == AccountType.REVENUE
                        || line.getAccount().getType() == AccountType.EXPENSE)
                .filter(line -> filter == null || line.getAccount().getType() == filter)
                .toList();
        List<Long> lineIds = lines.stream().map(BudgetLine::getId).toList();
        List<Long> periodIds = selectedPeriods.stream().map(AccountingPeriod::getId).toList();
        List<BudgetPeriodAllocation> allocations = lineIds.isEmpty() || periodIds.isEmpty()
                ? List.of() : allocationRepository
                .findByBudgetLineIdInAndAccountingPeriodIdIn(lineIds, periodIds);
        Map<Long, BigDecimal> budgetByLine = allocations.stream().collect(Collectors.groupingBy(
                a -> a.getBudgetLine().getId(), Collectors.reducing(BigDecimal.ZERO,
                        BudgetPeriodAllocation::getBudgetAmount, BigDecimal::add)));
        List<Account> accounts = lines.stream().map(BudgetLine::getAccount).toList();
        Map<Long, BigDecimal> actualByAccount =
                budgetActualService.getActualByAccounts(accounts, fromDate, toDate);
        List<BudgetVsActualLineDto> calculated = lines.stream()
                .map(line -> lineDto(line, budgetByLine.getOrDefault(line.getId(), BigDecimal.ZERO),
                        actualByAccount.getOrDefault(line.getAccount().getId(), BigDecimal.ZERO)))
                .sorted(Comparator.comparing(BudgetVsActualLineDto::getAccountCode)).toList();
        List<BudgetVsActualLineDto> revenue = calculated.stream()
                .filter(line -> line.getAccountType() == AccountType.REVENUE).toList();
        List<BudgetVsActualLineDto> expense = calculated.stream()
                .filter(line -> line.getAccountType() == AccountType.EXPENSE).toList();
        BigDecimal revenueBudget = sum(revenue, BudgetVsActualLineDto::getBudgetAmount);
        BigDecimal revenueActual = sum(revenue, BudgetVsActualLineDto::getActualAmount);
        BigDecimal expenseBudget = sum(expense, BudgetVsActualLineDto::getBudgetAmount);
        BigDecimal expenseActual = sum(expense, BudgetVsActualLineDto::getActualAmount);

        return BudgetVsActualResponseDto.builder()
                .budgetId(budget.getId()).budgetNumber(budget.getBudgetNumber())
                .budgetName(budget.getName()).budgetStatus(budget.getStatus())
                .fiscalYearId(budget.getFiscalYear().getId())
                .fiscalYearName(budget.getFiscalYear().getName()).currencyCode(resolveCurrency())
                .fromPeriodId(selectedPeriods.isEmpty() ? null : selectedPeriods.get(0).getId())
                .toPeriodId(selectedPeriods.isEmpty() ? null : selectedPeriods.get(selectedPeriods.size() - 1).getId())
                .selectedPeriodIds(periodIds).fromDate(fromDate).toDate(toDate)
                .totalRevenueBudget(revenueBudget).totalRevenueActual(revenueActual)
                .totalRevenueVariance(revenueActual.subtract(revenueBudget))
                .revenueAchievementPercent(percentage(revenueActual, revenueBudget))
                .totalExpenseBudget(expenseBudget).totalExpenseActual(expenseActual)
                .totalExpenseVariance(expenseBudget.subtract(expenseActual))
                .expenseUtilizationPercent(percentage(expenseActual, expenseBudget))
                .revenueLines(revenue).expenseLines(expense).generatedAt(LocalDateTime.now()).build();
    }

    private BudgetVsActualLineDto lineDto(BudgetLine line, BigDecimal budget, BigDecimal actual) {
        boolean revenue = line.getAccount().getType() == AccountType.REVENUE;
        BigDecimal variance = revenue ? actual.subtract(budget) : budget.subtract(actual);
        return BudgetVsActualLineDto.builder().budgetLineId(line.getId())
                .accountId(line.getAccount().getId()).accountCode(line.getAccount().getCode())
                .accountName(line.getAccount().getName()).accountType(line.getAccount().getType())
                .budgetAmount(budget).actualAmount(actual).varianceAmount(variance)
                .variancePercent(percentage(variance, budget))
                .utilizationPercent(percentage(actual, budget)).remainingAmount(budget.subtract(actual))
                .varianceStatus(variance.signum() > 0 ? VarianceStatus.FAVORABLE
                        : variance.signum() < 0 ? VarianceStatus.UNFAVORABLE : VarianceStatus.ON_TARGET)
                .build();
    }

    private List<AccountingPeriod> selectPeriods(
            List<AccountingPeriod> periods, Long fromPeriodId, Long toPeriodId) {
        if ((fromPeriodId == null) != (toPeriodId == null)) {
            throw new BusinessRuleException("fromPeriodId and toPeriodId must be supplied together");
        }
        if (fromPeriodId == null) return periods;
        int from = indexOf(periods, fromPeriodId);
        int to = indexOf(periods, toPeriodId);
        if (from > to) throw new BusinessRuleException("fromPeriodId must not be after toPeriodId");
        List<AccountingPeriod> selected = periods.subList(from, to + 1);
        for (int i = 1; i < selected.size(); i++) {
            AccountingPeriod previous = selected.get(i - 1);
            AccountingPeriod current = selected.get(i);
            if (!previous.getEndDate().plusDays(1).equals(current.getStartDate())
                    || current.getPeriodNumber() != previous.getPeriodNumber() + 1) {
                throw new BusinessRuleException("Selected accounting periods must be contiguous");
            }
        }
        return selected;
    }

    private int indexOf(List<AccountingPeriod> periods, Long id) {
        for (int i = 0; i < periods.size(); i++) {
            if (periods.get(i).getId().equals(id)) return i;
        }
        throw new BusinessRuleException("Accounting period does not belong to the budget fiscal year");
    }

    private List<AccountingPeriod> periodsFor(Budget budget) {
        List<AccountingPeriod> periods = accountingPeriodRepository
                .findByFiscalYearIdAndDeletedAtIsNullOrderByPeriodNumberAsc(budget.getFiscalYear().getId());
        if (periods.isEmpty()) throw new BusinessRuleException(
                "No accounting periods are configured for the budget fiscal year");
        return periods;
    }

    private Budget getBudget(Long budgetId) {
        if (budgetId == null) throw new BusinessRuleException("budgetId is required");
        return budgetRepository.findByIdAndDeletedAtIsNull(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
    }

    private BigDecimal percentage(BigDecimal numerator, BigDecimal denominator) {
        return denominator.compareTo(BigDecimal.ZERO) == 0 ? null
                : numerator.divide(denominator, 4, RoundingMode.HALF_UP).multiply(ONE_HUNDRED);
    }

    private BigDecimal sum(List<BudgetVsActualLineDto> lines,
                           java.util.function.Function<BudgetVsActualLineDto, BigDecimal> getter) {
        return lines.stream().map(getter).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String resolveCurrency() {
        try {
            String currency = systemSettingsService.getValue(SettingKey.DEFAULT_CURRENCY);
            return currency == null || currency.isBlank() ? "BDT" : currency.trim().toUpperCase(Locale.ROOT);
        } catch (RuntimeException ignored) {
            return "BDT";
        }
    }
}
