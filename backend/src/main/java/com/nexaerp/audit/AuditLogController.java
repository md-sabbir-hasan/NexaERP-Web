package com.nexaerp.audit;


import com.nexaerp.common.response.ApiResponse;
import com.nexaerp.common.response.PageResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {
    private final AuditLogService auditLogService;


    // Get all logs for a specific record
    @PreAuthorize("hasAuthority('VIEW_AUDIT_LOGS')")
    @GetMapping("/entity/{entityName}/{entityId}")
    public ResponseEntity<ApiResponse<PageResponseDto<AuditLog>>> getEntityHistory(
            @PathVariable String entityName,
            @PathVariable Long entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = pageable(page, size);
        return ResponseEntity.ok(ApiResponse.success(
                PageResponseDto.from(auditLogService.getEntityHistory(entityName, entityId, pageable))));
    }

    // Get all activity of a specific user
    @PreAuthorize("hasAuthority('VIEW_AUDIT_LOGS')")
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<PageResponseDto<AuditLog>>> getUserActivity(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = pageable(page, size);
        return ResponseEntity.ok(ApiResponse.success(
                PageResponseDto.from(auditLogService.getUserActivity(userId, pageable))));
    }

    // Get all logs for an entity type
    @PreAuthorize("hasAuthority('VIEW_AUDIT_LOGS')")
    @GetMapping("/entity/{entityName}")
    public ResponseEntity<ApiResponse<PageResponseDto<AuditLog>>> getEntityLogs(
            @PathVariable String entityName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = pageable(page, size);
        return ResponseEntity.ok(ApiResponse.success(
                PageResponseDto.from(auditLogService.getEntityLogs(entityName, pageable))));
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(page, size, Sort.by("createdAt").descending());
    }

}
