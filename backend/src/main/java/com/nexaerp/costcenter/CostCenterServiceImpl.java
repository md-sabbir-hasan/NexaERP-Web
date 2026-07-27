package com.nexaerp.costcenter;

import com.nexaerp.audit.AuditAction;
import com.nexaerp.audit.AuditLogService;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.costcenter.dto.CostCenterLookupDto;
import com.nexaerp.costcenter.dto.CostCenterRequestDto;
import com.nexaerp.costcenter.dto.CostCenterResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CostCenterServiceImpl implements CostCenterService {

    private final CostCenterRepository costCenterRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public CostCenterResponseDto create(CostCenterRequestDto request) {
        String code = normalizeCode(request.getCode());
        if (costCenterRepository.existsByCodeIgnoreCase(code)) {
            throw new BusinessRuleException("Cost center code already exists: " + code);
        }

        CostCenter saved = costCenterRepository.save(CostCenter.builder()
                .code(code)
                .name(request.getName().trim())
                .description(trimToNull(request.getDescription()))
                .isActive(true)
                .build());
        auditLogService.log(AuditAction.CREATED, "COST_CENTER", saved.getId(), null,
                saved.getCode() + " - " + saved.getName());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public CostCenterResponseDto update(Long id, CostCenterRequestDto request) {
        CostCenter costCenter = findOrThrow(id);
        String code = normalizeCode(request.getCode());
        if (!costCenter.getCode().equals(code)) {
            throw new BusinessRuleException("Cost center code cannot be changed after creation");
        }
        costCenter.setName(request.getName().trim());
        costCenter.setDescription(trimToNull(request.getDescription()));
        CostCenter saved = costCenterRepository.save(costCenter);
        auditLogService.log(AuditAction.UPDATED, "COST_CENTER", saved.getId(), null,
                saved.getCode() + " - " + saved.getName());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CostCenterResponseDto getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CostCenterResponseDto> getAll() {
        return costCenterRepository.findByDeletedAtIsNullOrderByCodeAsc().stream()
                .map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CostCenterResponseDto> search(String keyword, Boolean active) {
        String key = keyword == null ? "" : keyword.trim();
        List<CostCenter> results;
        if (!key.isEmpty() && active != null) {
            results = costCenterRepository
                    .findByIsActiveAndCodeContainingIgnoreCaseOrIsActiveAndNameContainingIgnoreCaseOrderByCodeAsc(
                            active, key, active, key);
        } else if (!key.isEmpty()) {
            results = costCenterRepository
                    .findByCodeContainingIgnoreCaseOrNameContainingIgnoreCaseOrderByCodeAsc(key, key);
        } else if (active != null) {
            results = costCenterRepository.findByIsActiveAndDeletedAtIsNullOrderByCodeAsc(active);
        } else {
            results = costCenterRepository.findByDeletedAtIsNullOrderByCodeAsc();
        }
        return results.stream().filter(costCenter -> !costCenter.isDeleted()).map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CostCenterLookupDto> lookup() {
        return costCenterRepository.findByIsActiveTrueAndDeletedAtIsNullOrderByCodeAsc().stream()
                .map(costCenter -> CostCenterLookupDto.builder()
                        .id(costCenter.getId()).code(costCenter.getCode()).name(costCenter.getName()).build())
                .toList();
    }

    @Override
    @Transactional
    public void activate(Long id) {
        CostCenter costCenter = findOrThrow(id);
        if (Boolean.TRUE.equals(costCenter.getIsActive())) {
            throw new BusinessRuleException("Cost center is already active");
        }
        costCenter.setIsActive(true);
        costCenterRepository.save(costCenter);
        auditLogService.log(AuditAction.ACTIVATED, "COST_CENTER", id, "INACTIVE", "ACTIVE");
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        CostCenter costCenter = findOrThrow(id);
        if (!Boolean.TRUE.equals(costCenter.getIsActive())) {
            throw new BusinessRuleException("Cost center is already inactive");
        }
        costCenter.setIsActive(false);
        costCenterRepository.save(costCenter);
        auditLogService.log(AuditAction.DEACTIVATED, "COST_CENTER", id, "ACTIVE", "INACTIVE");
    }

    @Override
    @Transactional(readOnly = true)
    public CostCenter resolveActive(Long id) {
        if (id == null) {
            return null;
        }
        CostCenter costCenter = findOrThrow(id);
        if (!Boolean.TRUE.equals(costCenter.getIsActive())) {
            throw new BusinessRuleException("Cannot use inactive cost center: " + costCenter.getCode());
        }
        return costCenter;
    }

    private CostCenter findOrThrow(Long id) {
        return costCenterRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cost center not found: " + id));
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private CostCenterResponseDto toResponse(CostCenter costCenter) {
        return CostCenterResponseDto.builder()
                .id(costCenter.getId())
                .code(costCenter.getCode())
                .name(costCenter.getName())
                .description(costCenter.getDescription())
                .isActive(costCenter.getIsActive())
                .createdAt(costCenter.getCreatedAt())
                .updatedAt(costCenter.getUpdatedAt())
                .build();
    }
}
