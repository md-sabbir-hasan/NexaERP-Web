package com.nexaerp.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {
    // Save a log entry
    void log(AuditAction action, String entityName, Long entityId,
             String oldValue, String newValue);

    // Get history of a specific record
    Page<AuditLog> getEntityHistory(String entityName, Long entityId, Pageable pageable);

    // Get activity of a specific user
    Page<AuditLog> getUserActivity(Long userId, Pageable pageable);

    // Get all logs for an entity type
    Page<AuditLog> getEntityLogs(String entityName, Pageable pageable);
}
