package com.nexaerp.costcenter;

import com.nexaerp.audit.AuditAction;
import com.nexaerp.audit.AuditLogService;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.costcenter.dto.CostCenterRequestDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CostCenterServiceImplTest {

    @Mock private CostCenterRepository repository;
    @Mock private AuditLogService auditLogService;
    @InjectMocks private CostCenterServiceImpl service;

    @Test
    void createNormalizesCodeAndAudits() {
        when(repository.save(any(CostCenter.class))).thenAnswer(invocation -> {
            CostCenter costCenter = invocation.getArgument(0);
            costCenter.setId(1L);
            return costCenter;
        });

        var response = service.create(new CostCenterRequestDto(" ops-01 ", " Operations ", " Main "));

        assertEquals("OPS-01", response.getCode());
        assertEquals("Operations", response.getName());
        assertTrue(response.getIsActive());
        verify(auditLogService).log(AuditAction.CREATED, "COST_CENTER", 1L, null,
                "OPS-01 - Operations");
    }

    @Test
    void duplicateCodeIsRejected() {
        when(repository.existsByCodeIgnoreCase("OPS")).thenReturn(true);
        assertThrows(BusinessRuleException.class,
                () -> service.create(new CostCenterRequestDto("ops", "Operations", null)));
    }

    @Test
    void codeIsImmutable() {
        CostCenter stored = CostCenter.builder().id(1L).code("OPS").name("Operations").isActive(true).build();
        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(stored));
        assertThrows(BusinessRuleException.class,
                () -> service.update(1L, new CostCenterRequestDto("NEW", "Operations", null)));
    }

    @Test
    void lookupReturnsOnlyActiveRepositoryResults() {
        when(repository.findByIsActiveTrueAndDeletedAtIsNullOrderByCodeAsc()).thenReturn(List.of(
                CostCenter.builder().id(1L).code("OPS").name("Operations").isActive(true).build()));
        assertEquals(List.of("OPS"), service.lookup().stream().map(dto -> dto.getCode()).toList());
    }

    @Test
    void inactiveAssignmentIsRejectedButCanBeReadHistorically() {
        CostCenter inactive = CostCenter.builder().id(1L).code("OPS").name("Operations").isActive(false).build();
        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(inactive));
        assertEquals("OPS", service.getById(1L).getCode());
        assertThrows(BusinessRuleException.class, () -> service.resolveActive(1L));
    }
}
