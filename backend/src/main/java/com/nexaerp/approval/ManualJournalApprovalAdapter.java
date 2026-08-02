package com.nexaerp.approval;

import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.journal.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ManualJournalApprovalAdapter implements ApprovalDocumentAdapter {
    private final ApprovalProperties properties;
    private final JournalEntryRepository repository;

    @Override public ApprovalEntityType entityType() { return ApprovalEntityType.MANUAL_JOURNAL; }
    @Override public boolean isEnabled() { return properties.isEnabled() && properties.getManualJournal().isEnabled(); }
    @Override public String requiredPermission() { return "APPROVE_JOURNAL"; }
    @Override public String viewPermission() { return "VIEW_JOURNAL"; }
    @Override public String displayName() { return "Journal"; }
    @Override public Object lockDocument(Long id) { return repository.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Journal entry not found")); }
    @Override public Object loadDocument(Long id) { return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Journal entry not found")); }
    @Override public void validateForSubmission(Object value) {
        JournalEntry journal = cast(value);
        if (journal.getSourceType() != JournalSourceType.MANUAL) throw rule("Only MANUAL journals can be submitted");
        if (journal.getStatus() != JournalStatus.DRAFT) throw rule("Only DRAFT journals can be submitted");
        if (journal.getLines() == null || journal.getLines().size() < 2) throw rule("Journal must contain at least two lines");
        BigDecimal debit = journal.getLines().stream().map(JournalLine::getDebit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credit = journal.getLines().stream().map(JournalLine::getCredit).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (debit.compareTo(credit) != 0) throw rule("Journal must be balanced before submission");
    }
    @Override public void validatePending(Object value, ApprovalRequest request) {
        JournalEntry journal = cast(value);
        if (journal.getSourceType() != JournalSourceType.MANUAL || journal.getStatus() != JournalStatus.DRAFT) throw rule("Journal is no longer an eligible MANUAL DRAFT");
        if (!Objects.equals(journal.getUpdatedAt(), request.getDocumentUpdatedAt())) throw rule("Journal changed after submission; submit it again");
    }
    @Override public LocalDateTime approve(Object document, Long actorId) { return cast(document).getUpdatedAt(); }
    @Override public Long creatorId(Object document) { return cast(document).getCreatedBy(); }
    @Override public LocalDateTime updatedAt(Object document) { return cast(document).getUpdatedAt(); }
    @Override public String documentNumber(Object document) { return cast(document).getEntryNumber(); }
    @Override public String documentTitle(Object document) { return cast(document).getDescription(); }
    @Override public String documentUrl(Long id) { return "/journals/" + id + "/edit"; }
    private JournalEntry cast(Object value) { return (JournalEntry) value; }
    private BusinessRuleException rule(String message) { return new BusinessRuleException(message); }
}
