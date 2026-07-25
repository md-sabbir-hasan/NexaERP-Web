package com.nexaerp.audit;

import com.nexaerp.audit.dto.AuditTimelineItemDto;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService{

    private static final Set<String> TIMELINE_ENTITY_ALLOWLIST = Set.of(
            "INVOICE",
            "VENDOR_BILL"
    );

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;


    @Override
    public void log(AuditAction action, String entityName, Long entityId, String oldValue, String newValue) {

        // Get current user from SecurityContext
        Long userId = null;
        String userName = "SYSTEM";

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() &&
                !auth.getPrincipal().equals("anonymousUser")) {

            String email = auth.getName();

            var userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                userId = userOpt.get().getId();
                userName = userOpt.get().getName();
            }
        }

        // Get IP address
        String ipAddress = getIpAddress();

        AuditLog log = AuditLog.builder()
                .userId(userId)
                .userName(userName)
                .entityName(entityName)
                .entityId(entityId)
                .action(action)
                .oldValue(AuditMasker.mask(oldValue))
                .newValue(AuditMasker.mask(newValue))
                .ipAddress(ipAddress)
                .build();

        auditLogRepository.save(log);
    }

    @Override
    public Page<AuditLog> getEntityHistory(String entityName, Long entityId, Pageable pageable) {
        return auditLogRepository
                .findByEntityNameAndEntityIdOrderByCreatedAtDesc(entityName, entityId, pageable);
    }

    @Override
    public Page<AuditLog> getUserActivity(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Override
    public Page<AuditLog> getEntityLogs(String entityName, Pageable pageable) {
        return auditLogRepository.findByEntityNameOrderByCreatedAtDesc(entityName, pageable);
    }

    @Override
    public Page<AuditTimelineItemDto> getEntityTimeline(
            String entityName,
            Long entityId,
            Pageable pageable
    ) {
        if (!TIMELINE_ENTITY_ALLOWLIST.contains(entityName)) {
            throw new BusinessRuleException(
                    "Activity timeline is not supported for entity: " + entityName
            );
        }

        return auditLogRepository
                .findByEntityNameAndEntityIdOrderByCreatedAtDesc(entityName, entityId, pageable)
                .map(this::toTimelineItem);
    }



    // ===================private helper==============


    private String getIpAddress() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty()) {
                    ip = request.getRemoteAddr();
                }
                return ip;
            }
        } catch (Exception e) {
            // ignore
        }
        return "unknown";
    }

    private AuditTimelineItemDto toTimelineItem(AuditLog auditLog) {
        return AuditTimelineItemDto.builder()
                .id(auditLog.getId())
                .entityName(auditLog.getEntityName())
                .entityId(auditLog.getEntityId())
                .action(auditLog.getAction())
                .actorName(auditLog.getUserName())
                .description(timelineDescription(auditLog.getEntityName(), auditLog.getAction()))
                .createdAt(auditLog.getCreatedAt())
                .build();
    }

    private String timelineDescription(String entityName, AuditAction action) {
        String subject = switch (entityName) {
            case "INVOICE" -> "Invoice";
            case "VENDOR_BILL" -> "Vendor bill";
            default -> "Record";
        };

        return switch (action) {
            case CREATED -> subject + " was created";
            case UPDATED -> subject + " was updated";
            case APPROVED -> subject + " was approved";
            case POSTED -> subject + " was posted";
            case CANCELLED -> subject + " was cancelled";
            default -> subject + " was " + action.name()
                    .toLowerCase(Locale.ROOT)
                    .replace('_', ' ');
        };
    }
}
