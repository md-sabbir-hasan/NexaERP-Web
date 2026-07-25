package com.nexaerp.accountingperiod;

import com.nexaerp.accountingperiod.dto.AccountingPeriodRequestDto;
import com.nexaerp.accountingperiod.dto.AccountingPeriodResponseDto;
import com.nexaerp.accountingperiod.dto.PeriodCloseChecklistResponseDto;
import com.nexaerp.audit.AuditAction;
import com.nexaerp.audit.AuditLogService;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.fiscalyear.FiscalYear;
import com.nexaerp.fiscalyear.FiscalYearRepository;
import com.nexaerp.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountingPeriodServiceImpl implements AccountingPeriodService {

    private static final String ENTITY_NAME = "ACCOUNTING_PERIOD";

    private final AccountingPeriodRepository accountingPeriodRepository;
    private final FiscalYearRepository fiscalYearRepository;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final PeriodCloseValidationService periodCloseValidationService;

    @Override
    @Transactional
    public AccountingPeriodResponseDto create(AccountingPeriodRequestDto request) {
        FiscalYear fiscalYear = getFiscalYear(request.getFiscalYearId());
        validateFiscalYearCanAcceptPeriods(fiscalYear);
        validateRequest(request, null, fiscalYear);

        AccountingPeriod period = AccountingPeriod.builder()
                .fiscalYear(fiscalYear)
                .name(request.getName().trim())
                .periodNumber(request.getPeriodNumber())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(AccountingPeriodStatus.OPEN)
                .remarks(trimToNull(request.getRemarks()))
                .build();

        AccountingPeriod saved = accountingPeriodRepository.save(period);
        auditLogService.log(AuditAction.CREATED, ENTITY_NAME, saved.getId(), null, auditValue(saved));
        return toResponse(saved);
    }

    @Override
    @Transactional
    public List<AccountingPeriodResponseDto> generateMonthlyPeriods(Long fiscalYearId) {
        FiscalYear fiscalYear = getFiscalYear(fiscalYearId);
        validateFiscalYearCanAcceptPeriods(fiscalYear);

        if (!accountingPeriodRepository
                .findByFiscalYearIdAndDeletedAtIsNullOrderByPeriodNumberAsc(fiscalYearId)
                .isEmpty()) {
            throw new BusinessRuleException(
                    "Accounting periods already exist for fiscal year " + fiscalYear.getName()
            );
        }

        LocalDate cursor = fiscalYear.getStartDate();
        int periodNumber = 1;

        while (!cursor.isAfter(fiscalYear.getEndDate())) {
            YearMonth yearMonth = YearMonth.from(cursor);
            LocalDate periodStart = cursor;
            LocalDate periodEnd = yearMonth.atEndOfMonth();

            if (periodEnd.isAfter(fiscalYear.getEndDate())) {
                periodEnd = fiscalYear.getEndDate();
            }

            AccountingPeriod period = AccountingPeriod.builder()
                    .fiscalYear(fiscalYear)
                    .name(yearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                            + " " + yearMonth.getYear())
                    .periodNumber(periodNumber++)
                    .startDate(periodStart)
                    .endDate(periodEnd)
                    .status(AccountingPeriodStatus.OPEN)
                    .build();

            AccountingPeriod saved = accountingPeriodRepository.save(period);
            auditLogService.log(AuditAction.CREATED, ENTITY_NAME, saved.getId(), null, auditValue(saved));
            cursor = periodEnd.plusDays(1);
        }

        return accountingPeriodRepository
                .findByFiscalYearIdAndDeletedAtIsNullOrderByPeriodNumberAsc(fiscalYearId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AccountingPeriodResponseDto update(Long id, AccountingPeriodRequestDto request) {
        AccountingPeriod period = getPeriod(id);

        if (period.getStatus() != AccountingPeriodStatus.OPEN) {
            throw new BusinessRuleException("Only an open accounting period can be edited");
        }

        FiscalYear fiscalYear = getFiscalYear(request.getFiscalYearId());
        validateFiscalYearCanAcceptPeriods(fiscalYear);
        validateRequest(request, id, fiscalYear);

        String oldValue = auditValue(period);

        period.setFiscalYear(fiscalYear);
        period.setName(request.getName().trim());
        period.setPeriodNumber(request.getPeriodNumber());
        period.setStartDate(request.getStartDate());
        period.setEndDate(request.getEndDate());
        period.setRemarks(trimToNull(request.getRemarks()));

        AccountingPeriod saved = accountingPeriodRepository.save(period);
        auditLogService.log(AuditAction.UPDATED, ENTITY_NAME, saved.getId(), oldValue, auditValue(saved));
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountingPeriodResponseDto getById(Long id) {
        return toResponse(getPeriod(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountingPeriodResponseDto> getAll(Long fiscalYearId) {
        List<AccountingPeriod> periods = fiscalYearId == null
                ? accountingPeriodRepository.findByDeletedAtIsNullOrderByStartDateDesc()
                : accountingPeriodRepository
                .findByFiscalYearIdAndDeletedAtIsNullOrderByPeriodNumberAsc(fiscalYearId);

        return periods.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AccountingPeriodResponseDto getCurrent(LocalDate date) {
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        return toResponse(findSinglePeriodForDate(targetDate));
    }

    @Override
    @Transactional
    public AccountingPeriodResponseDto open(Long id, String remarks) {
        AccountingPeriod period = getPeriod(id);

        if (period.getStatus() == AccountingPeriodStatus.OPEN) {
            throw new BusinessRuleException("Accounting period is already open");
        }

        if (period.getStatus() == AccountingPeriodStatus.LOCKED) {
            throw new BusinessRuleException("Locked accounting period cannot be reopened");
        }

        validateFiscalYearCanAcceptPeriods(period.getFiscalYear());
        String oldValue = auditValue(period);

        period.setStatus(AccountingPeriodStatus.OPEN);
        period.setClosedAt(null);
        period.setClosedBy(null);
        if (remarks != null) {
            period.setRemarks(trimToNull(remarks));
        }

        AccountingPeriod saved = accountingPeriodRepository.save(period);
        auditLogService.log(AuditAction.OPENED, ENTITY_NAME, saved.getId(), oldValue, auditValue(saved));
        return toResponse(saved);
    }

    @Override
    @Transactional
    public AccountingPeriodResponseDto close(Long id, String remarks) {
        AccountingPeriod period = getPeriod(id);

        if (period.getStatus() == AccountingPeriodStatus.CLOSED) {
            throw new BusinessRuleException("Accounting period is already closed");
        }

        if (period.getStatus() == AccountingPeriodStatus.LOCKED) {
            throw new BusinessRuleException("Locked accounting period cannot be closed again");
        }

        PeriodCloseChecklistResponseDto checklist = periodCloseValidationService.runChecklist(id);
        if (!checklist.isAllPassed()) {
            String pendingItems = checklist.getChecks().stream()
                    .filter(check -> !check.isPassed())
                    .map(check -> check.getName() + " (" + check.getCount() + ")")
                    .collect(Collectors.joining(", "));
            throw new BusinessRuleException("Cannot close period. Pending items found: " + pendingItems);
        }

        String oldValue = auditValue(period);
        period.setStatus(AccountingPeriodStatus.CLOSED);
        period.setClosedAt(LocalDateTime.now());
        period.setClosedBy(getCurrentUserId());
        if (remarks != null) {
            period.setRemarks(trimToNull(remarks));
        }

        AccountingPeriod saved = accountingPeriodRepository.save(period);
        auditLogService.log(AuditAction.CLOSED, ENTITY_NAME, saved.getId(), oldValue, auditValue(saved));
        return toResponse(saved);
    }

    @Override
    @Transactional
    public AccountingPeriodResponseDto lock(Long id, String remarks) {
        AccountingPeriod period = getPeriod(id);

        if (period.getStatus() != AccountingPeriodStatus.CLOSED) {
            throw new BusinessRuleException("Only a closed accounting period can be locked");
        }

        String oldValue = auditValue(period);
        period.setStatus(AccountingPeriodStatus.LOCKED);
        period.setLockedAt(LocalDateTime.now());
        period.setLockedBy(getCurrentUserId());
        if (remarks != null) {
            period.setRemarks(trimToNull(remarks));
        }

        AccountingPeriod saved = accountingPeriodRepository.save(period);
        auditLogService.log(AuditAction.LOCKED, ENTITY_NAME, saved.getId(), oldValue, auditValue(saved));
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PeriodCloseChecklistResponseDto getCloseChecklist(Long id) {
        return periodCloseValidationService.runChecklist(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        AccountingPeriod period = getPeriod(id);

        if (period.getStatus() != AccountingPeriodStatus.OPEN) {
            throw new BusinessRuleException("Only an open accounting period can be deleted");
        }

        String oldValue = auditValue(period);
        period.setDeletedAt(LocalDateTime.now());
        accountingPeriodRepository.save(period);
        auditLogService.log(AuditAction.DELETED, ENTITY_NAME, period.getId(), oldValue, null);
    }

    @Override
    @Transactional(readOnly = true)
    public void validatePostingDate(LocalDate postingDate) {
        if (postingDate == null) {
            throw new BusinessRuleException("Posting date is required");
        }

        AccountingPeriod period = findSinglePeriodForDate(postingDate);

        if (period.getStatus() != AccountingPeriodStatus.OPEN) {
            throw new BusinessRuleException(
                    "Posting is not allowed. Accounting period '" + period.getName() + "' is "
                            + period.getStatus().name().toLowerCase()
            );
        }

        if (!"ACTIVE".equalsIgnoreCase(String.valueOf(period.getFiscalYear().getStatus()))) {
            throw new BusinessRuleException(
                    "Posting is not allowed. Fiscal year '" + period.getFiscalYear().getName()
                            + "' is not active"
            );
        }
    }

    private void validateRequest(
            AccountingPeriodRequestDto request,
            Long existingId,
            FiscalYear fiscalYear
    ) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessRuleException("End date cannot be before start date");
        }

        if (request.getStartDate().isBefore(fiscalYear.getStartDate())
                || request.getEndDate().isAfter(fiscalYear.getEndDate())) {
            throw new BusinessRuleException("Accounting period must be inside the fiscal year date range");
        }

        boolean duplicateNumber = existingId == null
                ? accountingPeriodRepository
                .existsByFiscalYearIdAndPeriodNumberAndDeletedAtIsNull(
                        fiscalYear.getId(), request.getPeriodNumber())
                : accountingPeriodRepository
                .existsByFiscalYearIdAndPeriodNumberAndIdNotAndDeletedAtIsNull(
                        fiscalYear.getId(), request.getPeriodNumber(), existingId);

        if (duplicateNumber) {
            throw new BusinessRuleException(
                    "Period number " + request.getPeriodNumber() + " already exists in this fiscal year"
            );
        }

        if (accountingPeriodRepository.existsOverlappingPeriod(
                fiscalYear.getId(), request.getStartDate(), request.getEndDate(), existingId)) {
            throw new BusinessRuleException("Accounting period dates overlap with an existing period");
        }
    }

    private void validateFiscalYearCanAcceptPeriods(FiscalYear fiscalYear) {
        if ("CLOSED".equalsIgnoreCase(String.valueOf(fiscalYear.getStatus()))) {
            throw new BusinessRuleException("Closed fiscal year cannot be modified");
        }
    }

    private AccountingPeriod findSinglePeriodForDate(LocalDate date) {
        List<AccountingPeriod> periods = accountingPeriodRepository.findPeriodsContainingDate(date);

        if (periods.isEmpty()) {
            throw new BusinessRuleException(
                    "No accounting period is configured for posting date " + date
            );
        }

        if (periods.size() > 1) {
            throw new BusinessRuleException(
                    "Multiple accounting periods contain posting date " + date
                            + ". Please fix overlapping period configuration"
            );
        }

        return periods.getFirst();
    }

    private AccountingPeriod getPeriod(Long id) {
        return accountingPeriodRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Accounting period not found"));
    }

    private FiscalYear getFiscalYear(Long id) {
        return fiscalYearRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fiscal year not found"));
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        return userRepository.findByEmail(authentication.getName())
                .map(user -> user.getId())
                .orElse(null);
    }

    private AccountingPeriodResponseDto toResponse(AccountingPeriod period) {
        LocalDate today = LocalDate.now();

        return AccountingPeriodResponseDto.builder()
                .id(period.getId())
                .fiscalYearId(period.getFiscalYear().getId())
                .fiscalYearName(period.getFiscalYear().getName())
                .name(period.getName())
                .periodNumber(period.getPeriodNumber())
                .startDate(period.getStartDate())
                .endDate(period.getEndDate())
                .status(period.getStatus())
                .future(period.getStartDate().isAfter(today))
                .current(!today.isBefore(period.getStartDate()) && !today.isAfter(period.getEndDate()))
                .closedAt(period.getClosedAt())
                .closedBy(period.getClosedBy())
                .lockedAt(period.getLockedAt())
                .lockedBy(period.getLockedBy())
                .remarks(period.getRemarks())
                .createdAt(period.getCreatedAt())
                .updatedAt(period.getUpdatedAt())
                .build();
    }

    private String auditValue(AccountingPeriod period) {
        return "{" +
                "\"fiscalYearId\":" + period.getFiscalYear().getId() + "," +
                "\"name\":\"" + escape(period.getName()) + "\"," +
                "\"periodNumber\":" + period.getPeriodNumber() + "," +
                "\"startDate\":\"" + period.getStartDate() + "\"," +
                "\"endDate\":\"" + period.getEndDate() + "\"," +
                "\"status\":\"" + period.getStatus() + "\"" +
                "}";
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}