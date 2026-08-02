package com.nexaerp.approval;

import com.nexaerp.approval.dto.*;
import com.nexaerp.audit.*;
import com.nexaerp.common.exception.*;
import com.nexaerp.common.response.PageResponseDto;
import com.nexaerp.journal.*;
import com.nexaerp.notification.*;
import com.nexaerp.security.CurrentUserService;
import com.nexaerp.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.security.core.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ApprovalServiceImpl implements ApprovalService {
    private static final String APPROVE_JOURNAL = "APPROVE_JOURNAL";
    private static final String VIEW_QUEUE = "VIEW_APPROVAL_QUEUE";
    private static final int ACTIVE = 1;

    private final ApprovalProperties properties;
    private final ApprovalRequestRepository requestRepository;
    private final ApprovalActionRepository actionRepository;
    private final JournalEntryRepository journalRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    @Override public boolean isManualJournalApprovalEnabled() {
        return properties.isEnabled() && properties.getManualJournal().isEnabled();
    }

    @Override @Transactional(readOnly = true)
    public ApprovalRequest findLatestJournalRequest(Long journalId) {
        if (!isManualJournalApprovalEnabled()) return null;
        return requestRepository.findTopByEntityTypeAndEntityIdOrderBySubmittedAtDesc(
                ApprovalEntityType.MANUAL_JOURNAL, journalId).orElse(null);
    }

    @Override
    @Transactional
    public ApprovalRequestResponseDto submitManualJournal(Long journalId) {
        requireEnabled();
        JournalEntry journal = getJournal(journalId);
        Long actorId = currentUserService.getCurrentUserId();
        if (journal.getSourceType() != JournalSourceType.MANUAL) throw rule("Only MANUAL journals can be submitted");
        if (journal.getStatus() != JournalStatus.DRAFT) throw rule("Only DRAFT journals can be submitted");
        if (!actorId.equals(journal.getCreatedBy())) throw rule("Only the journal creator can submit it for approval");
        validateJournalLines(journal);
        if (requestRepository.findByEntityTypeAndEntityIdAndActiveMarker(ApprovalEntityType.MANUAL_JOURNAL, journalId, ACTIVE).isPresent())
            throw rule("An active approval request already exists for this journal");

        ApprovalRequest previous = requestRepository.findTopByEntityTypeAndEntityIdOrderBySubmittedAtDesc(
                ApprovalEntityType.MANUAL_JOURNAL, journalId).orElse(null);
        if (previous != null && (previous.getStatus() == ApprovalStatus.REJECTED || previous.getStatus() == ApprovalStatus.RETURNED)
                && !journal.getUpdatedAt().isAfter(previous.getDocumentUpdatedAt()))
            throw rule("Journal must be corrected before it can be resubmitted");

        ApprovalRequest request = ApprovalRequest.builder()
                .entityType(ApprovalEntityType.MANUAL_JOURNAL).entityId(journalId)
                .documentNumber(journal.getEntryNumber()).documentTitle(cleanTitle(journal.getDescription()))
                .makerUserId(actorId).status(ApprovalStatus.PENDING).requiredPermission(APPROVE_JOURNAL)
                .documentUpdatedAt(journal.getUpdatedAt()).submittedAt(LocalDateTime.now()).activeMarker(ACTIVE)
                .supersedesRequestId(previous != null && previous.getActiveMarker() == null ? previous.getId() : null).build();
        try { request = requestRepository.saveAndFlush(request); }
        catch (DataIntegrityViolationException ex) { throw rule("An active approval request already exists for this journal"); }
        addAction(request, ApprovalActionType.SUBMITTED, null, ApprovalStatus.PENDING, null, actorId);
        auditLogService.log(AuditAction.SUBMITTED, "APPROVAL_REQUEST", request.getId(), null, request.getDocumentNumber());
        List<Long> approvers = userRepository.findDistinctByStatusAndPermissionCode(UserStatus.ACTIVE, APPROVE_JOURNAL)
                .stream().map(User::getId).filter(id -> !id.equals(actorId)).toList();
        notificationService.scheduleUniqueForUsersAfterCommit(approvers, NotificationType.APPROVAL_SUBMITTED,
                NotificationPriority.MEDIUM, NotificationModule.APPROVAL, "Journal approval requested",
                "Journal " + request.getDocumentNumber() + " is waiting for approval.", "/approvals/" + request.getId(),
                "APPROVAL_REQUEST", request.getId());
        return toResponse(request, true);
    }

    @Override @Transactional public ApprovalRequestResponseDto approve(Long id, ApprovalDecisionDto dto) {
        return decide(id, dto, ApprovalStatus.APPROVED, ApprovalActionType.APPROVED, AuditAction.APPROVED, NotificationType.APPROVAL_APPROVED);
    }
    @Override @Transactional public ApprovalRequestResponseDto reject(Long id, ApprovalDecisionDto dto) {
        requireComment(dto); return decide(id, dto, ApprovalStatus.REJECTED, ApprovalActionType.REJECTED, AuditAction.REJECTED, NotificationType.APPROVAL_REJECTED);
    }
    @Override @Transactional public ApprovalRequestResponseDto returnForCorrection(Long id, ApprovalDecisionDto dto) {
        requireComment(dto); return decide(id, dto, ApprovalStatus.RETURNED, ApprovalActionType.RETURNED, AuditAction.RETURNED, NotificationType.APPROVAL_RETURNED);
    }

    private ApprovalRequestResponseDto decide(Long id, ApprovalDecisionDto dto, ApprovalStatus target,
            ApprovalActionType action, AuditAction audit, NotificationType notificationType) {
        requireEnabled();
        ApprovalRequest request = requestRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Approval request not found"));
        if (request.getStatus() != ApprovalStatus.PENDING) throw rule("Approval request is no longer pending");
        User actor = activeActor();
        requireAuthority(request.getRequiredPermission());
        if (actor.getId().equals(request.getMakerUserId())) throw rule("Maker cannot decide their own approval request");
        JournalEntry journal = validateCurrentJournal(request);
        String comment = dto == null ? null : clean(dto.getComment());
        ApprovalStatus from = request.getStatus();
        request.setStatus(target); request.setDecidedAt(LocalDateTime.now()); request.setDecidedBy(actor.getId());
        request.setDecisionComment(comment); request.setActiveMarker(target == ApprovalStatus.APPROVED ? ACTIVE : null);
        requestRepository.save(request);
        addAction(request, action, from, target, comment, actor.getId());
        auditLogService.log(audit, "APPROVAL_REQUEST", request.getId(), from.name(), target.name());
        NotificationPriority priority = target == ApprovalStatus.APPROVED ? NotificationPriority.MEDIUM : NotificationPriority.HIGH;
        String verb = target.name().toLowerCase(Locale.ROOT);
        notificationService.scheduleUniqueForUserAfterCommit(request.getMakerUserId(), notificationType, priority,
                NotificationModule.APPROVAL, "Journal approval " + verb,
                "Journal " + journal.getEntryNumber() + " was " + verb + ".", "/approvals/" + request.getId(),
                "APPROVAL_REQUEST", request.getId());
        return toResponse(request, true);
    }

    @Override @Transactional(readOnly = true) public PageResponseDto<ApprovalRequestResponseDto> pending(int page, int size) {
        requireEnabled(); requireAuthority(VIEW_QUEUE);
        Long userId = currentUserService.getCurrentUserId();
        List<String> permissions = authorities();
        if (permissions.isEmpty()) return PageResponseDto.from(Page.empty(PageRequest.of(page, size)));
        return PageResponseDto.from(requestRepository.findPendingForUser(userId, permissions, PageRequest.of(page, size))
                .map(r -> toResponse(r, false)));
    }
    @Override @Transactional(readOnly = true) public long pendingCount() {
        requireEnabled(); requireAuthority(VIEW_QUEUE);
        List<String> permissions = authorities();
        return permissions.isEmpty() ? 0 : requestRepository.countPendingForUser(currentUserService.getCurrentUserId(), permissions);
    }
    @Override @Transactional(readOnly = true) public PageResponseDto<ApprovalRequestResponseDto> myRequests(int p, int s) {
        requireEnabled(); return PageResponseDto.from(requestRepository.findByMakerUserIdOrderBySubmittedAtDesc(
                currentUserService.getCurrentUserId(), PageRequest.of(p, s)).map(r -> toResponse(r, false)));
    }
    @Override @Transactional(readOnly = true) public PageResponseDto<ApprovalActionResponseDto> myActions(int p, int s) {
        requireEnabled(); return PageResponseDto.from(actionRepository.findByActorUserIdOrderByCreatedAtDesc(
                currentUserService.getCurrentUserId(), PageRequest.of(p, s)).map(this::toAction));
    }
    @Override @Transactional(readOnly = true) public ApprovalRequestResponseDto getById(Long id) {
        requireEnabled(); ApprovalRequest r = requestRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Approval request not found"));
        authorizeRead(r, false); return toResponse(r, true);
    }
    @Override @Transactional(readOnly = true) public List<ApprovalRequestResponseDto> history(ApprovalEntityType type, Long entityId) {
        requireEnabled(); if (type != ApprovalEntityType.MANUAL_JOURNAL) throw rule("Unsupported approval entity type");
        requireAuthority("VIEW_JOURNAL"); getJournal(entityId);
        return requestRepository.findByEntityTypeAndEntityIdOrderBySubmittedAtDesc(type, entityId).stream().map(r -> toResponse(r, true)).toList();
    }

    @Override @Transactional(readOnly = true) public void assertJournalChangeAllowed(Long journalId) {
        if (!isManualJournalApprovalEnabled()) return;
        if (requestRepository.findByEntityTypeAndEntityIdAndActiveMarker(ApprovalEntityType.MANUAL_JOURNAL, journalId, ACTIVE).isPresent())
            throw rule("Journal cannot be changed while approval is pending or approved");
    }

    @Override public ApprovalRequest lockAndValidateForPosting(Long journalId) {
        if (!isManualJournalApprovalEnabled()) return null;
        ApprovalRequest request = requestRepository.findActiveForUpdate(ApprovalEntityType.MANUAL_JOURNAL, journalId)
                .orElseThrow(() -> rule("Journal requires approval before posting"));
        if (request.getStatus() != ApprovalStatus.APPROVED || request.getConsumedAt() != null)
            throw rule("Journal requires an unconsumed approved request before posting");
        validateCurrentJournal(request);
        return request;
    }

    @Override public void consumeAfterSuccessfulPost(ApprovalRequest request) {
        if (request == null) return;
        ApprovalRequest locked = requestRepository.findByIdForUpdate(request.getId()).orElseThrow(() -> rule("Approval request no longer exists"));
        if (locked.getStatus() != ApprovalStatus.APPROVED || locked.getConsumedAt() != null || locked.getActiveMarker() == null)
            throw rule("Approval request has already been consumed");
        Long actorId = currentUserService.getCurrentUserId();
        locked.setConsumedAt(LocalDateTime.now()); locked.setConsumedBy(actorId); locked.setActiveMarker(null);
        requestRepository.save(locked);
        addAction(locked, ApprovalActionType.CONSUMED, ApprovalStatus.APPROVED, ApprovalStatus.APPROVED, null, actorId);
        auditLogService.log(AuditAction.CONSUMED, "APPROVAL_REQUEST", locked.getId(), "APPROVED", "CONSUMED");
    }

    private JournalEntry validateCurrentJournal(ApprovalRequest request) {
        if (request.getEntityType() != ApprovalEntityType.MANUAL_JOURNAL) throw rule("Unsupported approval entity type");
        JournalEntry journal = getJournal(request.getEntityId());
        if (journal.getSourceType() != JournalSourceType.MANUAL || journal.getStatus() != JournalStatus.DRAFT)
            throw rule("Journal is no longer an eligible MANUAL DRAFT");
        if (!Objects.equals(journal.getUpdatedAt(), request.getDocumentUpdatedAt())) throw rule("Journal changed after submission; submit it again");
        return journal;
    }
    private void validateJournalLines(JournalEntry j) {
        if (j.getLines() == null || j.getLines().size() < 2) throw rule("Journal must contain at least two lines");
        BigDecimal debit = j.getLines().stream().map(JournalLine::getDebit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credit = j.getLines().stream().map(JournalLine::getCredit).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (debit.compareTo(credit) != 0) throw rule("Journal must be balanced before submission");
    }
    private void addAction(ApprovalRequest r, ApprovalActionType type, ApprovalStatus from, ApprovalStatus to, String comment, Long actorId) {
        User actor = userRepository.findById(actorId).orElseThrow(() -> rule("Approval actor was not found"));
        actionRepository.save(ApprovalAction.builder().approvalRequest(r).action(type).actorUserId(actorId)
                .actorNameSnapshot(actor.getName()).fromStatus(from).toStatus(to).comment(comment).build());
    }
    private User activeActor() {
        User user = userRepository.findById(currentUserService.getCurrentUserId()).orElseThrow(() -> rule("Approval actor was not found"));
        if (user.getStatus() != UserStatus.ACTIVE) throw rule("Only ACTIVE users can perform approval actions");
        return user;
    }
    private void authorizeRead(ApprovalRequest r, boolean documentHistory) {
        Long id = currentUserService.getCurrentUserId();
        if (id.equals(r.getMakerUserId())) return;
        List<String> auth = authorities();
        if (auth.contains(VIEW_QUEUE) && auth.contains(r.getRequiredPermission())) return;
        if (documentHistory && auth.contains("VIEW_JOURNAL")) return;
        throw rule("Approval request is not visible to the current user");
    }
    private List<String> authorities() { Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a == null ? List.of() : a.getAuthorities().stream().map(GrantedAuthority::getAuthority).distinct().toList(); }
    private void requireAuthority(String value) { if (!authorities().contains(value)) throw rule("Required permission is missing: " + value); }
    private void requireEnabled() { if (!isManualJournalApprovalEnabled()) throw rule("Manual Journal approval workflow is disabled"); }
    private void requireComment(ApprovalDecisionDto dto) { if (dto == null || clean(dto.getComment()) == null) throw rule("Decision comment is required"); }
    private JournalEntry getJournal(Long id) { return journalRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Journal entry not found")); }
    private BusinessRuleException rule(String m) { return new BusinessRuleException(m); }
    private String clean(String value) { if (value == null) return null; String v = value.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", " ").trim(); return v.isEmpty() ? null : v.substring(0, Math.min(500, v.length())); }
    private String cleanTitle(String value) { String v = clean(value); return v == null ? null : v.substring(0, Math.min(255, v.length())); }
    private ApprovalRequestResponseDto toResponse(ApprovalRequest r, boolean actions) {
        String maker = userRepository.findById(r.getMakerUserId()).map(User::getName).orElse("Unknown user");
        return ApprovalRequestResponseDto.builder().id(r.getId()).entityType(r.getEntityType()).entityId(r.getEntityId())
                .documentNumber(r.getDocumentNumber()).documentTitle(r.getDocumentTitle()).makerUserId(r.getMakerUserId()).makerName(maker)
                .status(r.getStatus()).requiredPermission(r.getRequiredPermission()).submittedAt(r.getSubmittedAt()).decidedAt(r.getDecidedAt())
                .decidedBy(r.getDecidedBy()).decisionComment(r.getDecisionComment()).consumedAt(r.getConsumedAt()).consumedBy(r.getConsumedBy())
                .supersedesRequestId(r.getSupersedesRequestId()).canDecide(r.getStatus() == ApprovalStatus.PENDING && !r.getMakerUserId().equals(currentUserService.getCurrentUserId()) && authorities().contains(r.getRequiredPermission()))
                .actions(actions ? actionRepository.findByApprovalRequestIdOrderByCreatedAtAscIdAsc(r.getId()).stream().map(this::toAction).toList() : List.of()).build();
    }
    private ApprovalActionResponseDto toAction(ApprovalAction a) { return ApprovalActionResponseDto.builder().id(a.getId())
            .approvalRequestId(a.getApprovalRequest().getId()).action(a.getAction()).actorUserId(a.getActorUserId()).actorName(a.getActorNameSnapshot())
            .fromStatus(a.getFromStatus()).toStatus(a.getToStatus()).comment(a.getComment()).createdAt(a.getCreatedAt()).build(); }
}
