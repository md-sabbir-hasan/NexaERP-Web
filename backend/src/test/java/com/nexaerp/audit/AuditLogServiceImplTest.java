package com.nexaerp.audit;

import com.nexaerp.audit.dto.AuditTimelineItemDto;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuditLogServiceImplTest {

    private AuditLogRepository auditLogRepository;
    private AuditLogServiceImpl auditLogService;

    @BeforeEach
    void setUp() {
        auditLogRepository = mock(AuditLogRepository.class);
        auditLogService = new AuditLogServiceImpl(
                auditLogRepository,
                mock(UserRepository.class)
        );
    }

    @Test
    void mapsSupportedInvoiceTimelineAndDelegatesPagination() {
        Pageable pageable = PageRequest.of(1, 20);
        AuditLog log = auditLog(
                10L,
                "INVOICE",
                42L,
                AuditAction.POSTED,
                "Amina Rahman"
        );
        when(auditLogRepository.findByEntityNameAndEntityIdOrderByCreatedAtDesc(
                "INVOICE",
                42L,
                pageable
        )).thenReturn(new PageImpl<>(List.of(log), pageable, 21));

        Page<AuditTimelineItemDto> result = auditLogService.getEntityTimeline(
                "INVOICE",
                42L,
                pageable
        );

        AuditTimelineItemDto item = result.getContent().getFirst();
        assertEquals("INVOICE", item.getEntityName());
        assertEquals(42L, item.getEntityId());
        assertEquals(AuditAction.POSTED, item.getAction());
        assertEquals("Amina Rahman", item.getActorName());
        assertEquals("Invoice was posted", item.getDescription());
        assertEquals(21, result.getTotalElements());
        verify(auditLogRepository).findByEntityNameAndEntityIdOrderByCreatedAtDesc(
                "INVOICE",
                42L,
                pageable
        );
    }

    @Test
    void mapsVendorBillDescriptionsAndFallsBackForUnknownSupportedAction() {
        Pageable pageable = PageRequest.of(0, 20);
        List<AuditLog> logs = List.of(
                auditLog(1L, "VENDOR_BILL", 7L, AuditAction.CREATED, "Maker"),
                auditLog(2L, "VENDOR_BILL", 7L, AuditAction.APPROVED, "Checker"),
                auditLog(3L, "VENDOR_BILL", 7L, AuditAction.LOCKED, "Checker")
        );
        when(auditLogRepository.findByEntityNameAndEntityIdOrderByCreatedAtDesc(
                "VENDOR_BILL",
                7L,
                pageable
        )).thenReturn(new PageImpl<>(logs, pageable, logs.size()));

        List<String> descriptions = auditLogService
                .getEntityTimeline("VENDOR_BILL", 7L, pageable)
                .map(AuditTimelineItemDto::getDescription)
                .getContent();

        assertEquals(List.of(
                "Vendor bill was created",
                "Vendor bill was approved",
                "Vendor bill was locked"
        ), descriptions);
    }

    @Test
    void rejectsUnsupportedEntityBeforeRepositoryAccess() {
        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> auditLogService.getEntityTimeline(
                        "PAYMENT",
                        5L,
                        PageRequest.of(0, 20)
                )
        );

        assertEquals(
                "Activity timeline is not supported for entity: PAYMENT",
                exception.getMessage()
        );
        verifyNoInteractions(auditLogRepository);
    }

    @Test
    void timelineDtoContractExcludesSensitiveAuditFields() {
        Set<String> fieldNames = Arrays.stream(AuditTimelineItemDto.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                "id",
                "entityName",
                "entityId",
                "action",
                "actorName",
                "description",
                "createdAt"
        ), fieldNames);
        assertFalse(fieldNames.contains("oldValue"));
        assertFalse(fieldNames.contains("newValue"));
        assertFalse(fieldNames.contains("ipAddress"));
        assertFalse(fieldNames.contains("userId"));
    }

    private AuditLog auditLog(
            Long id,
            String entityName,
            Long entityId,
            AuditAction action,
            String userName
    ) {
        return AuditLog.builder()
                .id(id)
                .entityName(entityName)
                .entityId(entityId)
                .action(action)
                .userName(userName)
                .createdAt(LocalDateTime.of(2026, 7, 25, 12, 0))
                .oldValue("sensitive-old")
                .newValue("sensitive-new")
                .ipAddress("127.0.0.1")
                .build();
    }
}
