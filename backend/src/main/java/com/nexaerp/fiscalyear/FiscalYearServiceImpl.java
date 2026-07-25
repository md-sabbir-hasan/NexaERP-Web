package com.nexaerp.fiscalyear;

import com.nexaerp.audit.AuditAction;
import com.nexaerp.audit.AuditLogService;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.fiscalyear.dto.FiscalYearRequestDto;
import com.nexaerp.fiscalyear.dto.FiscalYearResponseDto;
import com.nexaerp.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FiscalYearServiceImpl implements FiscalYearService {

    private final FiscalYearRepository fiscalYearRepository;
    private final AuditLogService auditLogService;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public FiscalYearResponseDto create(FiscalYearRequestDto request) {

        validateNameForCreate(request.getName());
        validateDateRange(
                request.getStartDate(),
                request.getEndDate()
        );

        boolean overlaps =
                fiscalYearRepository.existsOverlappingPeriod(
                        request.getStartDate(),
                        request.getEndDate()
                );

        if (overlaps) {
            throw new BusinessRuleException(
                    "Fiscal year date range overlaps an existing fiscal year"
            );
        }

        FiscalYear fiscalYear = FiscalYear.builder()
                .name(request.getName().trim())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(FiscalYearStatus.DRAFT)
                .description(trimToNull(request.getDescription()))
                .build();

        FiscalYear saved =
                fiscalYearRepository.save(fiscalYear);

        auditLogService.log(
                AuditAction.CREATED,
                "FISCAL_YEAR",
                saved.getId(),
                null,
                saved.getName()
        );

        return toResponse(saved);
    }

    @Override
    @Transactional
    public FiscalYearResponseDto update(
            Long id,
            FiscalYearRequestDto request
    ) {

        FiscalYear fiscalYear = findActiveRecord(id);

        if (fiscalYear.getStatus() == FiscalYearStatus.CLOSED) {
            throw new BusinessRuleException(
                    "Closed fiscal year cannot be updated"
            );
        }

        validateNameForUpdate(request.getName(), id);
        validateDateRange(
                request.getStartDate(),
                request.getEndDate()
        );

        boolean overlaps =
                fiscalYearRepository
                        .existsOverlappingPeriodExcludingId(
                                id,
                                request.getStartDate(),
                                request.getEndDate()
                        );

        if (overlaps) {
            throw new BusinessRuleException(
                    "Fiscal year date range overlaps an existing fiscal year"
            );
        }

        String oldValue =
                fiscalYear.getName()
                        + " ["
                        + fiscalYear.getStartDate()
                        + " to "
                        + fiscalYear.getEndDate()
                        + "]";

        fiscalYear.setName(request.getName().trim());
        fiscalYear.setStartDate(request.getStartDate());
        fiscalYear.setEndDate(request.getEndDate());
        fiscalYear.setDescription(
                trimToNull(request.getDescription())
        );

        FiscalYear saved =
                fiscalYearRepository.save(fiscalYear);

        String newValue =
                saved.getName()
                        + " ["
                        + saved.getStartDate()
                        + " to "
                        + saved.getEndDate()
                        + "]";

        auditLogService.log(
                AuditAction.UPDATED,
                "FISCAL_YEAR",
                saved.getId(),
                oldValue,
                newValue
        );

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public FiscalYearResponseDto getById(Long id) {
        return toResponse(findActiveRecord(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FiscalYearResponseDto> getAll() {

        return fiscalYearRepository
                .findByDeletedAtIsNullOrderByStartDateDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FiscalYearResponseDto getActive() {

        FiscalYear fiscalYear =
                fiscalYearRepository
                        .findFirstByStatusAndDeletedAtIsNull(
                                FiscalYearStatus.ACTIVE
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "No active fiscal year found"
                                )
                        );

        return toResponse(fiscalYear);
    }

    @Override
    @Transactional(readOnly = true)
    public FiscalYearResponseDto getByDate(LocalDate date) {

        if (date == null) {
            throw new BusinessRuleException(
                    "Date is required"
            );
        }

        List<FiscalYear> matches =
                fiscalYearRepository.findContainingDate(date);

        if (matches.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No fiscal year found for date " + date
            );
        }

        if (matches.size() > 1) {
            throw new BusinessRuleException(
                    "Multiple fiscal years contain date " + date
            );
        }

        return toResponse(matches.getFirst());
    }

    @Override
    @Transactional
    public FiscalYearResponseDto activate(Long id) {

        FiscalYear fiscalYear = findActiveRecord(id);

        if (fiscalYear.getStatus() == FiscalYearStatus.ACTIVE) {
            throw new BusinessRuleException(
                    "Fiscal year is already active"
            );
        }

        if (fiscalYear.getStatus() == FiscalYearStatus.CLOSED) {
            throw new BusinessRuleException(
                    "Closed fiscal year cannot be activated"
            );
        }

        fiscalYearRepository
                .findFirstByStatusAndDeletedAtIsNull(
                        FiscalYearStatus.ACTIVE
                )
                .ifPresent(active -> {
                    throw new BusinessRuleException(
                            "Another fiscal year is already active: "
                                    + active.getName()
                    );
                });

        FiscalYearStatus oldStatus = fiscalYear.getStatus();

        fiscalYear.setStatus(FiscalYearStatus.ACTIVE);
        fiscalYear.setActivatedAt(LocalDateTime.now());
        fiscalYear.setActivatedBy(
                currentUserService.getCurrentUserId()
        );

        FiscalYear saved =
                fiscalYearRepository.save(fiscalYear);

        auditLogService.log(
                AuditAction.ACTIVATED,
                "FISCAL_YEAR",
                saved.getId(),
                oldStatus.name(),
                FiscalYearStatus.ACTIVE.name()
        );

        return toResponse(saved);
    }

    @Override
    @Transactional
    public FiscalYearResponseDto close(Long id) {

        FiscalYear fiscalYear = findActiveRecord(id);

        if (fiscalYear.getStatus() == FiscalYearStatus.CLOSED) {
            throw new BusinessRuleException(
                    "Fiscal year is already closed"
            );
        }

        if (fiscalYear.getStatus() != FiscalYearStatus.ACTIVE) {
            throw new BusinessRuleException(
                    "Only an ACTIVE fiscal year can be closed"
            );
        }

        fiscalYear.setStatus(FiscalYearStatus.CLOSED);
        fiscalYear.setClosedAt(LocalDateTime.now());
        fiscalYear.setClosedBy(
                currentUserService.getCurrentUserId()
        );

        FiscalYear saved =
                fiscalYearRepository.save(fiscalYear);

        auditLogService.log(
                AuditAction.CLOSED,
                "FISCAL_YEAR",
                saved.getId(),
                FiscalYearStatus.ACTIVE.name(),
                FiscalYearStatus.CLOSED.name()
        );

        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {

        FiscalYear fiscalYear = findActiveRecord(id);

        if (fiscalYear.getStatus() != FiscalYearStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Only DRAFT fiscal years can be deleted"
            );
        }

        fiscalYear.setDeletedAt(LocalDateTime.now());

        FiscalYear saved =
                fiscalYearRepository.save(fiscalYear);

        auditLogService.log(
                AuditAction.DELETED,
                "FISCAL_YEAR",
                saved.getId(),
                saved.getName(),
                null
        );
    }

    private FiscalYear findActiveRecord(Long id) {

        return fiscalYearRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Fiscal year not found"
                        )
                );
    }

    private void validateNameForCreate(String name) {

        String normalized =
                name == null ? "" : name.trim();

        if (normalized.isBlank()) {
            throw new BusinessRuleException(
                    "Fiscal year name is required"
            );
        }

        boolean exists =
                fiscalYearRepository
                        .existsByNameIgnoreCaseAndDeletedAtIsNull(
                                normalized
                        );

        if (exists) {
            throw new BusinessRuleException(
                    "Fiscal year name already exists"
            );
        }
    }

    private void validateNameForUpdate(
            String name,
            Long id
    ) {

        String normalized =
                name == null ? "" : name.trim();

        if (normalized.isBlank()) {
            throw new BusinessRuleException(
                    "Fiscal year name is required"
            );
        }

        boolean exists =
                fiscalYearRepository
                        .existsByNameIgnoreCaseAndIdNotAndDeletedAtIsNull(
                                normalized,
                                id
                        );

        if (exists) {
            throw new BusinessRuleException(
                    "Fiscal year name already exists"
            );
        }
    }

    private void validateDateRange(
            LocalDate startDate,
            LocalDate endDate
    ) {

        if (startDate == null || endDate == null) {
            throw new BusinessRuleException(
                    "Fiscal year start date and end date are required"
            );
        }

        if (!endDate.isAfter(startDate)) {
            throw new BusinessRuleException(
                    "Fiscal year end date must be after start date"
            );
        }
    }

    private FiscalYearResponseDto toResponse(
            FiscalYear fiscalYear
    ) {

        LocalDate today = LocalDate.now();

        boolean current =
                fiscalYear.getStatus() == FiscalYearStatus.ACTIVE
                        && !today.isBefore(
                        fiscalYear.getStartDate()
                )
                        && !today.isAfter(
                        fiscalYear.getEndDate()
                );

        return FiscalYearResponseDto.builder()
                .id(fiscalYear.getId())
                .name(fiscalYear.getName())
                .startDate(fiscalYear.getStartDate())
                .endDate(fiscalYear.getEndDate())
                .status(fiscalYear.getStatus())
                .description(fiscalYear.getDescription())
                .current(current)
                .activatedAt(fiscalYear.getActivatedAt())
                .activatedBy(fiscalYear.getActivatedBy())
                .closedAt(fiscalYear.getClosedAt())
                .closedBy(fiscalYear.getClosedBy())
                .createdAt(fiscalYear.getCreatedAt())
                .updatedAt(fiscalYear.getUpdatedAt())
                .build();
    }

    private String trimToNull(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}