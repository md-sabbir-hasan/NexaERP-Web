package com.nexaerp.approval;

import java.time.LocalDateTime;

/**
 * Document-specific behavior used by the single generic approval workflow.
 */
public interface ApprovalDocumentAdapter {
    ApprovalEntityType entityType();

    boolean isEnabled();

    String requiredPermission();

    /**
     * Permission needed to REJECT this document type.
     * Defaults to the approve permission for adapters that don't override it.
     */
    default String rejectPermission() {
        return requiredPermission();
    }

    /**
     * Permission needed to RETURN this document type for correction.
     * Defaults to the approve permission for adapters that don't override it.
     */
    default String returnPermission() {
        return requiredPermission();
    }

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
