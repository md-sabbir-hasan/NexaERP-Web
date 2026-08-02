package com.nexaerp.approval;

import java.time.LocalDateTime;

/** Document-specific behavior used by the single generic approval workflow. */
public interface ApprovalDocumentAdapter {
    ApprovalEntityType entityType();
    boolean isEnabled();
    String requiredPermission();
    String viewPermission();
    String displayName();
    Object lockDocument(Long id);
    Object loadDocument(Long id);
    void validateForSubmission(Object document);
    void validatePending(Object document, ApprovalRequest request);
    LocalDateTime approve(Object document, Long actorId);
    Long creatorId(Object document);
    LocalDateTime updatedAt(Object document);
    String documentNumber(Object document);
    String documentTitle(Object document);
    String documentUrl(Long id);
}
