package com.nexaerp.budget.serviceimpl;

import com.nexaerp.account.Account;
import com.nexaerp.account.AccountRepository;
import com.nexaerp.account.AccountType;
import com.nexaerp.accountingperiod.AccountingPeriod;
import com.nexaerp.accountingperiod.AccountingPeriodRepository;
import com.nexaerp.audit.AuditAction;
import com.nexaerp.audit.AuditLogService;
import com.nexaerp.budget.*;
import com.nexaerp.budget.dto.*;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.fiscalyear.FiscalYear;
import com.nexaerp.fiscalyear.FiscalYearRepository;
import com.nexaerp.fiscalyear.FiscalYearStatus;
import com.nexaerp.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository budgetRepository;
    private final BudgetLineRepository budgetLineRepository;
    private final BudgetPeriodAllocationRepository budgetPeriodAllocationRepository;
    private final AccountRepository accountRepository;
    private final FiscalYearRepository fiscalYearRepository;
    private final AccountingPeriodRepository accountingPeriodRepository;
    private final AuditLogService auditLogService;
    private final CurrentUserService currentUserService;
    private final BudgetActualService budgetActualService;

    @Override
    @Transactional
    public BudgetResponseDto create(BudgetCreateRequestDto request) {

        FiscalYear fiscalYear = fiscalYearRepository.findByIdAndDeletedAtIsNull(request.getFiscalYearId())
                .orElseThrow(() -> new ResourceNotFoundException("Fiscal year not found"));

        if (fiscalYear.getStatus() == FiscalYearStatus.CLOSED) {
            throw new BusinessRuleException("Cannot create a budget for a closed fiscal year");
        }

        int existingCount = budgetRepository
                .findByFiscalYearIdAndDeletedAtIsNullOrderByVersionNumberDesc(fiscalYear.getId())
                .size();

        Budget budget = Budget.builder()
                .budgetNumber(generateBudgetNumber())
                .fiscalYear(fiscalYear)
                .versionNumber(existingCount + 1)
                .name(request.getName())
                .description(request.getDescription())
                .status(BudgetStatus.DRAFT)
                .totalRevenueBudget(BigDecimal.ZERO)
                .totalExpenseBudget(BigDecimal.ZERO)
                .build();

        Budget saved = budgetRepository.save(budget);

        auditLogService.log(
                AuditAction.CREATED,
                "BUDGET",
                saved.getId(),
                null,
                saved.getBudgetNumber() + " - " + saved.getName()
        );

        return toResponse(saved);
    }

    @Override
    @Transactional
    public BudgetResponseDto update(Long id, BudgetUpdateRequestDto request) {
        Budget budget = findOrThrow(id);
        ensureEditable(budget);

        budget.setName(request.getName());
        budget.setDescription(request.getDescription());

        return toResponse(budgetRepository.save(budget));
    }

    @Override
    public BudgetResponseDto getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    public List<BudgetResponseDto> getAll() {
        return budgetRepository.findByDeletedAtIsNullOrderByCreatedAtDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<BudgetResponseDto> getByFiscalYear(Long fiscalYearId) {
        return budgetRepository.findByFiscalYearIdAndDeletedAtIsNullOrderByVersionNumberDesc(fiscalYearId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Budget budget = findOrThrow(id);
        ensureEditable(budget);

        budget.setDeletedAt(LocalDateTime.now());
        budgetRepository.save(budget);

        auditLogService.log(
                AuditAction.DELETED,
                "BUDGET",
                budget.getId(),
                budget.getBudgetNumber(),
                null
        );
    }

    @Override
    @Transactional
    public BudgetLineResponseDto addLine(Long budgetId, BudgetLineRequestDto request) {
        Budget budget = findOrThrow(budgetId);
        ensureEditable(budget);

        Account account = getAccount(request.getAccountId());
        validateBudgetableAccount(account);

        if (budgetLineRepository.existsByBudgetIdAndAccountId(budgetId, account.getId())) {
            throw new BusinessRuleException("A budget line already exists for account " + account.getName());
        }

        BudgetLine line = BudgetLine.builder()
                .budget(budget)
                .account(account)
                .annualAmount(request.getAnnualAmount())
                .allocationMethod(request.getAllocationMethod())
                .notes(request.getNotes())
                .build();

        BudgetLine savedLine = budgetLineRepository.save(line);

        allocatePeriods(budget, savedLine, request);
        recalculateTotals(budget);

        return toLineResponse(savedLine);
    }

    @Override
    @Transactional
    public BudgetLineResponseDto updateLine(Long budgetId, Long lineId, BudgetLineRequestDto request) {
        Budget budget = findOrThrow(budgetId);
        ensureEditable(budget);

        BudgetLine line = budgetLineRepository.findById(lineId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget line not found"));

        if (!line.getBudget().getId().equals(budget.getId())) {
            throw new BusinessRuleException("Budget line does not belong to this budget");
        }

        Account account = getAccount(request.getAccountId());
        validateBudgetableAccount(account);

        if (!line.getAccount().getId().equals(account.getId())
                && budgetLineRepository.existsByBudgetIdAndAccountId(budgetId, account.getId())) {
            throw new BusinessRuleException("A budget line already exists for account " + account.getName());
        }

        line.setAccount(account);
        line.setAnnualAmount(request.getAnnualAmount());
        line.setAllocationMethod(request.getAllocationMethod());
        line.setNotes(request.getNotes());

        BudgetLine savedLine = budgetLineRepository.save(line);

        budgetPeriodAllocationRepository.deleteByBudgetLineId(savedLine.getId());
        allocatePeriods(budget, savedLine, request);
        recalculateTotals(budget);

        return toLineResponse(savedLine);
    }

    @Override
    @Transactional
    public void deleteLine(Long budgetId, Long lineId) {
        Budget budget = findOrThrow(budgetId);
        ensureEditable(budget);

        BudgetLine line = budgetLineRepository.findById(lineId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget line not found"));

        if (!line.getBudget().getId().equals(budget.getId())) {
            throw new BusinessRuleException("Budget line does not belong to this budget");
        }

        budgetPeriodAllocationRepository.deleteByBudgetLineId(line.getId());
        budgetLineRepository.delete(line);

        recalculateTotals(budget);
    }

    @Override
    @Transactional
    public BudgetResponseDto activate(Long id) {
        Budget budget = findOrThrow(id);

        if (budget.getStatus() == BudgetStatus.ACTIVE) {
            throw new BusinessRuleException("Budget is already active");
        }
        if (budget.getStatus() == BudgetStatus.CLOSED || budget.getStatus() == BudgetStatus.CANCELLED) {
            throw new BusinessRuleException("A closed or cancelled budget cannot be activated");
        }

        List<BudgetLine> lines = budgetLineRepository.findByBudgetId(budget.getId());
        if (lines.isEmpty()) {
            throw new BusinessRuleException("Cannot activate a budget with no lines");
        }

        if (budgetRepository.existsByFiscalYearIdAndStatusAndDeletedAtIsNull(
                budget.getFiscalYear().getId(), BudgetStatus.ACTIVE)) {
            throw new BusinessRuleException(
                    "Another budget is already active for this fiscal year. Close it first.");
        }

        if (budget.getFiscalYear().getStatus() != FiscalYearStatus.ACTIVE) {
            throw new BusinessRuleException("Fiscal year must be ACTIVE to activate its budget");
        }

        BudgetStatus oldStatus = budget.getStatus();

        budget.setStatus(BudgetStatus.ACTIVE);
        budget.setActivatedAt(LocalDateTime.now());
        budget.setActivatedBy(currentUserService.getCurrentUserId());

        Budget saved = budgetRepository.save(budget);

        auditLogService.log(
                AuditAction.ACTIVATED,
                "BUDGET",
                saved.getId(),
                oldStatus.name(),
                BudgetStatus.ACTIVE.name()
        );

        return toResponse(saved);
    }

    @Override
    @Transactional
    public BudgetResponseDto close(Long id) {
        Budget budget = findOrThrow(id);

        if (budget.getStatus() != BudgetStatus.ACTIVE) {
            throw new BusinessRuleException("Only an ACTIVE budget can be closed");
        }

        budget.setStatus(BudgetStatus.CLOSED);
        budget.setClosedAt(LocalDateTime.now());
        budget.setClosedBy(currentUserService.getCurrentUserId());

        Budget saved = budgetRepository.save(budget);

        auditLogService.log(
                AuditAction.CLOSED,
                "BUDGET",
                saved.getId(),
                BudgetStatus.ACTIVE.name(),
                BudgetStatus.CLOSED.name()
        );

        return toResponse(saved);
    }

    @Override
    public BudgetVarianceResponseDto getVariance(Long budgetId, Long periodId, LocalDate fromDate, LocalDate toDate) {
        Budget budget = findOrThrow(budgetId);
        List<BudgetLine> lines = budgetLineRepository.findByBudgetId(budget.getId());

        LocalDate rangeFrom;
        LocalDate rangeTo;

        if (periodId != null) {
            AccountingPeriod period = accountingPeriodRepository.findById(periodId)
                    .orElseThrow(() -> new ResourceNotFoundException("Accounting period not found"));
            rangeFrom = period.getStartDate();
            rangeTo = period.getEndDate();
        } else if (fromDate != null && toDate != null) {
            rangeFrom = fromDate;
            rangeTo = toDate;
        } else {
            rangeFrom = budget.getFiscalYear().getStartDate();
            rangeTo = budget.getFiscalYear().getEndDate();
        }

        List<Account> accounts = lines.stream().map(BudgetLine::getAccount).collect(Collectors.toList());
        Map<Long, BigDecimal> actuals = budgetActualService.getActualByAccounts(accounts, rangeFrom, rangeTo);

        List<BudgetVarianceLineDto> varianceLines = new ArrayList<>();

        BigDecimal totalRevenueBudget = BigDecimal.ZERO;
        BigDecimal totalRevenueActual = BigDecimal.ZERO;
        BigDecimal totalExpenseBudget = BigDecimal.ZERO;
        BigDecimal totalExpenseActual = BigDecimal.ZERO;

        for (BudgetLine line : lines) {
            BigDecimal budgetAmount = resolveBudgetAmount(line, periodId, rangeFrom, rangeTo);
            BigDecimal actualAmount = actuals.getOrDefault(line.getAccount().getId(), BigDecimal.ZERO);

            boolean isRevenue = line.getAccount().getType() == AccountType.REVENUE;

            // Expense: Budget - Actual (spending less is favorable)
            // Revenue: Actual - Budget (earning more is favorable)
            BigDecimal variance = isRevenue
                    ? actualAmount.subtract(budgetAmount)
                    : budgetAmount.subtract(actualAmount);

            BigDecimal variancePercent = budgetAmount.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : variance.divide(budgetAmount, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

            BigDecimal utilizationPercent = budgetAmount.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : actualAmount.divide(budgetAmount, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

            VarianceStatus status = variance.compareTo(BigDecimal.ZERO) > 0
                    ? VarianceStatus.FAVORABLE
                    : variance.compareTo(BigDecimal.ZERO) < 0
                      ? VarianceStatus.UNFAVORABLE
                      : VarianceStatus.ON_TARGET;

            varianceLines.add(BudgetVarianceLineDto.builder()
                    .accountId(line.getAccount().getId())
                    .accountCode(line.getAccount().getCode())
                    .accountName(line.getAccount().getName())
                    .accountType(line.getAccount().getType().name())
                    .budgetAmount(budgetAmount)
                    .actualAmount(actualAmount)
                    .varianceAmount(variance)
                    .variancePercent(variancePercent)
                    .varianceStatus(status)
                    .utilizationPercent(utilizationPercent)
                    .build());

            if (isRevenue) {
                totalRevenueBudget = totalRevenueBudget.add(budgetAmount);
                totalRevenueActual = totalRevenueActual.add(actualAmount);
            } else {
                totalExpenseBudget = totalExpenseBudget.add(budgetAmount);
                totalExpenseActual = totalExpenseActual.add(actualAmount);
            }
        }

        return BudgetVarianceResponseDto.builder()
                .budgetId(budget.getId())
                .budgetName(budget.getName())
                .fiscalYearId(budget.getFiscalYear().getId())
                .fiscalYearName(budget.getFiscalYear().getName())
                .fromDate(rangeFrom)
                .toDate(rangeTo)
                .totalRevenueBudget(totalRevenueBudget)
                .totalRevenueActual(totalRevenueActual)
                .totalExpenseBudget(totalExpenseBudget)
                .totalExpenseActual(totalExpenseActual)
                .lines(varianceLines)
                .build();
    }

    private BigDecimal resolveBudgetAmount(BudgetLine line, Long periodId, LocalDate rangeFrom, LocalDate rangeTo) {
        List<BudgetPeriodAllocation> allocations = budgetPeriodAllocationRepository.findByBudgetLineId(line.getId());

        if (periodId != null) {
            return allocations.stream()
                    .filter(a -> a.getAccountingPeriod().getId().equals(periodId))
                    .map(BudgetPeriodAllocation::getBudgetAmount)
                    .findFirst()
                    .orElse(BigDecimal.ZERO);
        }

        // sum allocations whose period falls fully within [rangeFrom, rangeTo]
        return allocations.stream()
                .filter(a -> !a.getAccountingPeriod().getStartDate().isBefore(rangeFrom)
                        && !a.getAccountingPeriod().getEndDate().isAfter(rangeTo))
                .map(BudgetPeriodAllocation::getBudgetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // _______ Private helpers __________

    private void ensureEditable(Budget budget) {
        if (budget.getStatus() != BudgetStatus.DRAFT) {
            throw new BusinessRuleException("Only a DRAFT budget can be edited or deleted");
        }
    }

    private void validateBudgetableAccount(Account account) {
        if (account.getType() != AccountType.EXPENSE && account.getType() != AccountType.REVENUE) {
            throw new BusinessRuleException("Budget can only be assigned to REVENUE or EXPENSE accounts");
        }
        if (!Boolean.TRUE.equals(account.getIsActive())) {
            throw new BusinessRuleException("Inactive account cannot be budgeted");
        }
        if (accountRepository.existsByParentId(account.getId())) {
            throw new BusinessRuleException(
                    "Budget cannot be assigned directly to a parent account — choose a leaf account");
        }
    }

    private void allocatePeriods(Budget budget, BudgetLine line, BudgetLineRequestDto request) {
        List<AccountingPeriod> periods = accountingPeriodRepository
                .findByFiscalYearIdAndDeletedAtIsNullOrderByPeriodNumberAsc(budget.getFiscalYear().getId());

        if (periods.isEmpty()) {
            throw new BusinessRuleException(
                    "No accounting periods found for this fiscal year — set up periods before adding budget lines");
        }

        if (request.getAllocationMethod() == BudgetAllocationMethod.EQUAL) {
            List<BigDecimal> amounts = distributeEqually(request.getAnnualAmount(), periods.size());

            for (int i = 0; i < periods.size(); i++) {
                saveAllocation(line, periods.get(i), amounts.get(i));
            }

        } else { // MANUAL
            if (request.getPeriodAmounts() == null || request.getPeriodAmounts().isEmpty()) {
                throw new BusinessRuleException("periodAmounts is required when allocationMethod = MANUAL");
            }

            Map<Long, AccountingPeriod> periodMap = periods.stream()
                    .collect(Collectors.toMap(AccountingPeriod::getId, p -> p));

            BigDecimal total = BigDecimal.ZERO;

            for (BudgetPeriodAmountRequestDto dto : request.getPeriodAmounts()) {
                AccountingPeriod period = periodMap.get(dto.getAccountingPeriodId());
                if (period == null) {
                    throw new BusinessRuleException(
                            "Accounting period " + dto.getAccountingPeriodId()
                                    + " does not belong to this budget's fiscal year");
                }
                saveAllocation(line, period, dto.getAmount());
                total = total.add(dto.getAmount());
            }

            if (total.compareTo(request.getAnnualAmount()) != 0) {
                throw new BusinessRuleException(
                        "Sum of period amounts (" + total + ") must equal the annual amount ("
                                + request.getAnnualAmount() + ")");
            }
        }
    }

    private void saveAllocation(BudgetLine line, AccountingPeriod period, BigDecimal amount) {
        BudgetPeriodAllocation allocation = BudgetPeriodAllocation.builder()
                .budgetLine(line)
                .accountingPeriod(period)
                .budgetAmount(amount)
                .build();
        budgetPeriodAllocationRepository.save(allocation);
    }

    /**
     * Distributes an annual amount evenly across N periods without losing paisa
     * to rounding — the last period absorbs the remainder.
     */
    private List<BigDecimal> distributeEqually(BigDecimal annualAmount, int periodCount) {
        BigDecimal normalAmount = annualAmount.divide(BigDecimal.valueOf(periodCount), 2, RoundingMode.DOWN);

        List<BigDecimal> amounts = new ArrayList<>();
        BigDecimal allocated = BigDecimal.ZERO;

        for (int i = 1; i <= periodCount; i++) {
            BigDecimal amount = (i == periodCount) ? annualAmount.subtract(allocated) : normalAmount;
            amounts.add(amount);
            allocated = allocated.add(amount);
        }

        return amounts;
    }

    private void recalculateTotals(Budget budget) {
        List<BudgetLine> lines = budgetLineRepository.findByBudgetId(budget.getId());

        BigDecimal expenseTotal = lines.stream()
                .filter(l -> l.getAccount().getType() == AccountType.EXPENSE)
                .map(BudgetLine::getAnnualAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal revenueTotal = lines.stream()
                .filter(l -> l.getAccount().getType() == AccountType.REVENUE)
                .map(BudgetLine::getAnnualAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        budget.setTotalExpenseBudget(expenseTotal);
        budget.setTotalRevenueBudget(revenueTotal);

        budgetRepository.save(budget);
    }

    private String generateBudgetNumber() {
        return budgetRepository.findTopByOrderByIdDesc()
                .map(last -> {
                    String lastNumber = last.getBudgetNumber().replace("BUD-", "");
                    int next = Integer.parseInt(lastNumber) + 1;
                    return String.format("BUD-%04d", next);
                })
                .orElse("BUD-0001");
    }

    private Account getAccount(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));
    }

    private Budget findOrThrow(Long id) {
        return budgetRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
    }

    private BudgetResponseDto toResponse(Budget b) {
        List<BudgetLine> lines = budgetLineRepository.findByBudgetId(b.getId());

        return BudgetResponseDto.builder()
                .id(b.getId())
                .budgetNumber(b.getBudgetNumber())
                .fiscalYearId(b.getFiscalYear().getId())
                .fiscalYearName(b.getFiscalYear().getName())
                .versionNumber(b.getVersionNumber())
                .revisedFromBudgetId(b.getRevisedFromBudgetId())
                .name(b.getName())
                .description(b.getDescription())
                .status(b.getStatus())
                .totalRevenueBudget(b.getTotalRevenueBudget())
                .totalExpenseBudget(b.getTotalExpenseBudget())
                .activatedAt(b.getActivatedAt())
                .closedAt(b.getClosedAt())
                .createdAt(b.getCreatedAt())
                .lines(lines.stream().map(this::toLineResponse).collect(Collectors.toList()))
                .build();
    }

    private BudgetLineResponseDto toLineResponse(BudgetLine line) {
        List<BudgetPeriodAllocation> allocations = budgetPeriodAllocationRepository.findByBudgetLineId(line.getId());

        List<BudgetPeriodAllocationResponseDto> allocationDtos = allocations.stream()
                .sorted((a, b) -> a.getAccountingPeriod().getPeriodNumber()
                        .compareTo(b.getAccountingPeriod().getPeriodNumber()))
                .map(a -> BudgetPeriodAllocationResponseDto.builder()
                        .accountingPeriodId(a.getAccountingPeriod().getId())
                        .periodName(a.getAccountingPeriod().getName())
                        .periodNumber(a.getAccountingPeriod().getPeriodNumber())
                        .startDate(a.getAccountingPeriod().getStartDate())
                        .endDate(a.getAccountingPeriod().getEndDate())
                        .budgetAmount(a.getBudgetAmount())
                        .build())
                .collect(Collectors.toList());

        return BudgetLineResponseDto.builder()
                .id(line.getId())
                .accountId(line.getAccount().getId())
                .accountCode(line.getAccount().getCode())
                .accountName(line.getAccount().getName())
                .accountType(line.getAccount().getType().name())
                .annualAmount(line.getAnnualAmount())
                .allocationMethod(line.getAllocationMethod())
                .notes(line.getNotes())
                .periodAllocations(allocationDtos)
                .build();
    }
}