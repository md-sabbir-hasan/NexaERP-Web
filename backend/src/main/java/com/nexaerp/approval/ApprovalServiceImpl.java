package com.nexaerp.approval;

import com.nexaerp.approval.dto.*;
import com.nexaerp.audit.*;
import com.nexaerp.common.exception.*;
import com.nexaerp.common.response.PageResponseDto;
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

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ApprovalServiceImpl implements ApprovalService {
    private static final String VIEW_QUEUE = "VIEW_APPROVAL_QUEUE";
    private static final int ACTIVE = 1;

    private final ApprovalProperties properties;
    private final ApprovalRequestRepository requestRepository;
    private final ApprovalActionRepository actionRepository;
    private final List<ApprovalDocumentAdapter> documentAdapters;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    @Override
    public boolean isManualJournalApprovalEnabled() {
        return adapter(ApprovalEntityType.MANUAL_JOURNAL).isEnabled();
    }

    @Override
    public boolean isVendorBillApprovalEnabled() {
        return adapter(ApprovalEntityType.VENDOR_BILL).isEnabled();
    }

    @Override
    public boolean isInvoiceApprovalEnabled() {
        return adapter(ApprovalEntityType.INVOICE).isEnabled();
    }

    @Override
    public boolean isPaymentApprovalEnabled() {
        return adapter(ApprovalEntityType.PAYMENT).isEnabled();
    }

    @Override
    @Transactional(readOnly = true)
    public ApprovalRequest findLatestJournalRequest(Long id) {
        return findLatest(ApprovalEntityType.MANUAL_JOURNAL, id);
    }

    @Override
    @Transactional(readOnly = true)
    public ApprovalRequest findLatestVendorBillRequest(Long id) {
        return findLatest(ApprovalEntityType.VENDOR_BILL, id);
    }

    @Override
    @Transactional(readOnly = true)
    public ApprovalRequest findLatestInvoiceRequest(Long id) {
        return findLatest(ApprovalEntityType.INVOICE, id);
    }

    @Override
    @Transactional(readOnly = true)
    public ApprovalRequest findLatestPaymentRequest(Long id) {
        return findLatest(ApprovalEntityType.PAYMENT, id);
    }

    private ApprovalRequest findLatest(ApprovalEntityType type, Long id) {
        if (!adapter(type).isEnabled()) return null;
        return requestRepository.findTopByEntityTypeAndEntityIdOrderBySubmittedAtDesc(type, id).orElse(null);
    }

    @Override
    @Transactional
    public ApprovalRequestResponseDto submitManualJournal(Long id) {
        return submit(ApprovalEntityType.MANUAL_JOURNAL, id);
    }

    @Override
    @Transactional
    public ApprovalRequestResponseDto submitVendorBill(Long id) {
        return submit(ApprovalEntityType.VENDOR_BILL, id);
    }

    @Override
    @Transactional
    public ApprovalRequestResponseDto submitInvoice(Long id) {
        return submit(ApprovalEntityType.INVOICE, id);
    }

    @Override
    @Transactional
    public ApprovalRequestResponseDto submitPayment(Long id) {
        return submit(ApprovalEntityType.PAYMENT, id);
    }

    private ApprovalRequestResponseDto submit(ApprovalEntityType type, Long entityId) {
        ApprovalDocumentAdapter adapter = enabledAdapter(type);
        Object document = adapter.lockDocument(entityId);
        adapter.validateForSubmission(document);
        Long actorId = currentUserService.getCurrentUserId();
        if (!Objects.equals(actorId, adapter.creatorId(document)))
            throw rule("Only the document creator can submit it for approval");
        if (requestRepository.findByEntityTypeAndEntityIdAndActiveMarker(type, entityId, ACTIVE).isPresent())
            throw rule("An active approval request already exists for this document");
        ApprovalRequest previous = requestRepository.findTopByEntityTypeAndEntityIdOrderBySubmittedAtDesc(type, entityId).orElse(null);
        if (previous != null && (previous.getStatus() == ApprovalStatus.REJECTED || previous.getStatus() == ApprovalStatus.RETURNED)
                && !adapter.updatedAt(document).isAfter(previous.getDocumentUpdatedAt()))
            throw rule("Document must be corrected before it can be resubmitted");
        ApprovalRequest request = ApprovalRequest.builder().entityType(type).entityId(entityId)
                .documentNumber(adapter.documentNumber(document)).documentTitle(cleanTitle(adapter.documentTitle(document)))
                .makerUserId(actorId).status(ApprovalStatus.PENDING).requiredPermission(adapter.requiredPermission())
                .documentUpdatedAt(adapter.updatedAt(document)).submittedAt(LocalDateTime.now()).activeMarker(ACTIVE)
                .supersedesRequestId(previous != null && previous.getActiveMarker() == null ? previous.getId() : null).build();
        try {
            request = requestRepository.saveAndFlush(request);
        } catch (DataIntegrityViolationException ex) {
            throw rule("An active approval request already exists for this document");
        }
        addAction(request, ApprovalActionType.SUBMITTED, null, ApprovalStatus.PENDING, null, actorId);
        auditLogService.log(AuditAction.SUBMITTED, "APPROVAL_REQUEST", request.getId(), null, request.getDocumentNumber());
        List<Long> approvers = userRepository.findDistinctByStatusAndPermissionCode(UserStatus.ACTIVE, adapter.requiredPermission())
                .stream().map(User::getId).filter(id -> !id.equals(actorId)).toList();
        notificationService.scheduleUniqueForUsersAfterCommit(approvers, NotificationType.APPROVAL_SUBMITTED,
                NotificationPriority.MEDIUM, NotificationModule.APPROVAL, adapter.displayName() + " approval requested",
                adapter.displayName() + " " + request.getDocumentNumber() + " is waiting for approval.", "/approvals/" + request.getId(),
                "APPROVAL_REQUEST", request.getId());
        return toResponse(request, true);
    }

    @Override
    @Transactional
    public ApprovalRequestResponseDto approve(Long id, ApprovalDecisionDto dto) {
        return decide(id, dto, ApprovalStatus.APPROVED, ApprovalActionType.APPROVED, AuditAction.APPROVED, NotificationType.APPROVAL_APPROVED);
    }

    @Override
    @Transactional
    public ApprovalRequestResponseDto reject(Long id, ApprovalDecisionDto dto) {
        requireComment(dto);
        return decide(id, dto, ApprovalStatus.REJECTED, ApprovalActionType.REJECTED, AuditAction.REJECTED, NotificationType.APPROVAL_REJECTED);
    }

    @Override
    @Transactional
    public ApprovalRequestResponseDto returnForCorrection(Long id, ApprovalDecisionDto dto) {
        requireComment(dto);
        return decide(id, dto, ApprovalStatus.RETURNED, ApprovalActionType.RETURNED, AuditAction.RETURNED, NotificationType.APPROVAL_RETURNED);
    }

    private ApprovalRequestResponseDto decide(Long id, ApprovalDecisionDto dto, ApprovalStatus target,
                                              ApprovalActionType action, AuditAction audit, NotificationType notificationType) {
        ApprovalRequest routing = requestRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Approval request not found"));
        ApprovalDocumentAdapter adapter = enabledAdapter(routing.getEntityType());
        Object document = adapter.lockDocument(routing.getEntityId());
        ApprovalRequest request = requestRepository.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Approval request not found"));
        validateIdentity(routing, request);
        return decideLocked(adapter, document, request, dto, target, action, audit, notificationType);
    }

    private ApprovalRequestResponseDto decideLocked(ApprovalDocumentAdapter adapter, Object document, ApprovalRequest request,
                                                    ApprovalDecisionDto dto, ApprovalStatus target, ApprovalActionType action, AuditAction audit, NotificationType notificationType) {
        if (request.getStatus() != ApprovalStatus.PENDING || request.getActiveMarker() == null)
            throw rule("Approval request is no longer pending");
        User actor = activeActor();
        requireAuthority(request.getRequiredPermission());
        if (!Objects.equals(request.getRequiredPermission(), adapter.requiredPermission()))
            throw rule("Approval request permission does not match its document type");
        if (actor.getId().equals(request.getMakerUserId()))
            throw rule("Maker cannot decide their own approval request");
        adapter.validatePending(document, request);
        String comment = dto == null ? null : clean(dto.getComment());
        ApprovalStatus from = request.getStatus();
        if (target == ApprovalStatus.APPROVED) request.setDocumentUpdatedAt(adapter.approve(document, actor.getId()));
        request.setStatus(target);
        request.setDecidedAt(LocalDateTime.now());
        request.setDecidedBy(actor.getId());
        request.setDecisionComment(comment);
        request.setActiveMarker(target == ApprovalStatus.APPROVED ? ACTIVE : null);
        requestRepository.save(request);
        addAction(request, action, from, target, comment, actor.getId());
        auditLogService.log(audit, "APPROVAL_REQUEST", request.getId(), from.name(), target.name());
        NotificationPriority priority = target == ApprovalStatus.APPROVED ? NotificationPriority.MEDIUM : NotificationPriority.HIGH;
        String verb = target.name().toLowerCase(Locale.ROOT);
        notificationService.scheduleUniqueForUserAfterCommit(request.getMakerUserId(), notificationType, priority,
                NotificationModule.APPROVAL, adapter.displayName() + " " + verb,
                adapter.displayName() + " " + request.getDocumentNumber() + " was " + (target == ApprovalStatus.RETURNED ? "returned for correction" : verb) + ".",
                "/approvals/" + request.getId(), "APPROVAL_REQUEST", request.getId());
        return toResponse(request, true);
    }

    @Override
    @Transactional
    public ApprovalRequestResponseDto approveVendorBillCompatibility(Long vendorBillId) {
        ApprovalDocumentAdapter adapter = enabledAdapter(ApprovalEntityType.VENDOR_BILL);
        Object document = adapter.lockDocument(vendorBillId);
        ApprovalRequest request = requestRepository.findActiveForUpdate(ApprovalEntityType.VENDOR_BILL, vendorBillId)
                .orElseThrow(() -> rule("Vendor bill must be submitted for approval before it can be approved"));
        return decideLocked(adapter, document, request, null, ApprovalStatus.APPROVED, ApprovalActionType.APPROVED,
                AuditAction.APPROVED, NotificationType.APPROVAL_APPROVED);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<ApprovalRequestResponseDto> pending(int page, int size) {
        requireMasterEnabled();
        requireAuthority(VIEW_QUEUE);
        Long userId = currentUserService.getCurrentUserId();
        List<String> permissions = authorities();
        if (permissions.isEmpty()) return PageResponseDto.from(Page.empty(PageRequest.of(page, size)));
        return PageResponseDto.from(requestRepository.findPendingForUser(userId, permissions, PageRequest.of(page, size)).map(r -> toResponse(r, false)));
    }

    @Override
    @Transactional(readOnly = true)
    public long pendingCount() {
        requireMasterEnabled();
        requireAuthority(VIEW_QUEUE);
        List<String> p = authorities();
        return p.isEmpty() ? 0 : requestRepository.countPendingForUser(currentUserService.getCurrentUserId(), p);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<ApprovalRequestResponseDto> myRequests(int p, int s) {
        requireMasterEnabled();
        return PageResponseDto.from(requestRepository.findByMakerUserIdOrderBySubmittedAtDesc(currentUserService.getCurrentUserId(), PageRequest.of(p, s)).map(r -> toResponse(r, false)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<ApprovalActionResponseDto> myActions(int p, int s) {
        requireMasterEnabled();
        return PageResponseDto.from(actionRepository.findByActorUserIdOrderByCreatedAtDesc(currentUserService.getCurrentUserId(), PageRequest.of(p, s)).map(this::toAction));
    }

    @Override
    @Transactional(readOnly = true)
    public ApprovalRequestResponseDto getById(Long id) {
        requireMasterEnabled();
        ApprovalRequest r = requestRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Approval request not found"));
        authorizeRead(r);
        return toResponse(r, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApprovalRequestResponseDto> history(ApprovalEntityType type, Long entityId) {
        ApprovalDocumentAdapter adapter = enabledAdapter(type);
        requireAuthority(adapter.viewPermission());
        adapter.loadDocument(entityId);
        return requestRepository.findByEntityTypeAndEntityIdOrderBySubmittedAtDesc(type, entityId).stream().map(r -> toResponse(r, true)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public void assertJournalChangeAllowed(Long id) {
        assertChangeAllowed(ApprovalEntityType.MANUAL_JOURNAL, id);
    }

    @Override
    @Transactional(readOnly = true)
    public void assertVendorBillChangeAllowed(Long id) {
        assertChangeAllowed(ApprovalEntityType.VENDOR_BILL, id);
    }

    @Override
    @Transactional
    public void assertInvoiceChangeAllowed(Long id) {
        if (adapter(ApprovalEntityType.INVOICE).isEnabled()
                && requestRepository.findActiveForUpdate(ApprovalEntityType.INVOICE, id).isPresent())
            throw rule("Document cannot be changed while approval is pending or approved");
    }

    @Override
    @Transactional
    public void assertPaymentChangeAllowed(Long id) {
        if (adapter(ApprovalEntityType.PAYMENT).isEnabled()
                && requestRepository.findActiveForUpdate(ApprovalEntityType.PAYMENT, id).isPresent())
            throw rule("Document cannot be changed while approval is pending or approved");
    }

    private void assertChangeAllowed(ApprovalEntityType type, Long id) {
        if (adapter(type).isEnabled() && requestRepository.findByEntityTypeAndEntityIdAndActiveMarker(type, id, ACTIVE).isPresent())
            throw rule("Document cannot be changed while approval is pending or approved");
    }

    @Override
    public ApprovalRequest lockAndValidateForPosting(Long id) {
        return lockAndValidateForPosting(ApprovalEntityType.MANUAL_JOURNAL, id);
    }

    @Override
    public ApprovalRequest lockAndValidateVendorBillForPosting(Long id) {
        return lockAndValidateForPosting(ApprovalEntityType.VENDOR_BILL, id);
    }

    @Override
    public ApprovalRequest lockAndValidateInvoiceForPosting(Long id) {
        return lockAndValidateForPosting(ApprovalEntityType.INVOICE, id);
    }

    @Override
    public ApprovalRequest lockAndValidatePaymentForPosting(Long id) {
        return lockAndValidateForPosting(ApprovalEntityType.PAYMENT, id);
    }

    private ApprovalRequest lockAndValidateForPosting(ApprovalEntityType type, Long id) {
        ApprovalDocumentAdapter adapter = adapter(type);
        if (!adapter.isEnabled()) return null;
        Object document = adapter.lockDocument(id);
        ApprovalRequest request = requestRepository.findActiveForUpdate(type, id).orElseThrow(() -> rule("Document requires approval before posting"));
        if (request.getStatus() != ApprovalStatus.APPROVED || request.getConsumedAt() != null || request.getActiveMarker() == null)
            throw rule("Document requires an unconsumed approved request before posting");
        if (!Objects.equals(adapter.updatedAt(document), request.getDocumentUpdatedAt()))
            throw rule("Document changed after approval; submit it again");
        return request;
    }

    @Override
    public ApprovalRequest lockActiveVendorBillForCancellation(Long id) {
        ApprovalDocumentAdapter adapter = adapter(ApprovalEntityType.VENDOR_BILL);
        if (!adapter.isEnabled()) return null;
        adapter.lockDocument(id);
        return requestRepository.findActiveForUpdate(ApprovalEntityType.VENDOR_BILL, id).orElse(null);
    }

    @Override
    public ApprovalRequest lockActiveInvoiceForCancellation(Long id) {
        ApprovalDocumentAdapter adapter = adapter(ApprovalEntityType.INVOICE);
        if (!adapter.isEnabled()) return null;
        adapter.lockDocument(id);
        return requestRepository.findActiveForUpdate(ApprovalEntityType.INVOICE, id).orElse(null);
    }

    @Override
    public ApprovalRequest lockActivePaymentForCancellation(Long id) {
        ApprovalDocumentAdapter adapter = adapter(ApprovalEntityType.PAYMENT);
        if (!adapter.isEnabled()) return null;
        adapter.lockDocument(id);
        return requestRepository.findActiveForUpdate(ApprovalEntityType.PAYMENT, id).orElse(null);
    }

    @Override
    public void cancelAfterSuccessfulDocumentCancellation(ApprovalRequest request) {
        if (request == null) return;
        if (request.getConsumedAt() != null) return;
        ApprovalStatus from = request.getStatus();
        Long actorId = currentUserService.getCurrentUserId();
        request.setStatus(ApprovalStatus.CANCELLED);
        request.setActiveMarker(null);
        request.setDecidedAt(LocalDateTime.now());
        request.setDecidedBy(actorId);
        requestRepository.save(request);
        addAction(request, ApprovalActionType.CANCELLED, from, ApprovalStatus.CANCELLED, null, actorId);
        auditLogService.log(AuditAction.CANCELLED, "APPROVAL_REQUEST", request.getId(), from.name(), ApprovalStatus.CANCELLED.name());
    }

    @Override
    public void consumeAfterSuccessfulPost(ApprovalRequest request) {
        if (request == null) return;
        ApprovalDocumentAdapter adapter = enabledAdapter(request.getEntityType());
        adapter.lockDocument(request.getEntityId());
        ApprovalRequest locked = requestRepository.findByIdForUpdate(request.getId()).orElseThrow(() -> rule("Approval request no longer exists"));
        if (locked.getStatus() != ApprovalStatus.APPROVED || locked.getConsumedAt() != null || locked.getActiveMarker() == null)
            throw rule("Approval request has already been consumed");
        Long actorId = currentUserService.getCurrentUserId();
        locked.setConsumedAt(LocalDateTime.now());
        locked.setConsumedBy(actorId);
        locked.setActiveMarker(null);
        requestRepository.save(locked);
        addAction(locked, ApprovalActionType.CONSUMED, ApprovalStatus.APPROVED, ApprovalStatus.APPROVED, null, actorId);
        auditLogService.log(AuditAction.CONSUMED, "APPROVAL_REQUEST", locked.getId(), "APPROVED", "CONSUMED");
    }

    private ApprovalDocumentAdapter adapter(ApprovalEntityType type) {
        return documentAdapters.stream().filter(a -> a.entityType() == type).findFirst().orElseThrow(() -> rule("Unsupported approval entity type"));
    }

    private ApprovalDocumentAdapter enabledAdapter(ApprovalEntityType type) {
        ApprovalDocumentAdapter a = adapter(type);
        if (!a.isEnabled()) throw rule(a.displayName() + " approval workflow is disabled");
        return a;
    }

    private void validateIdentity(ApprovalRequest routing, ApprovalRequest locked) {
        if (routing.getEntityType() != locked.getEntityType() || !Objects.equals(routing.getEntityId(), locked.getEntityId()))
            throw rule("Approval request changed while it was being processed");
    }

    private void addAction(ApprovalRequest r, ApprovalActionType type, ApprovalStatus from, ApprovalStatus to, String comment, Long actorId) {
        User actor = userRepository.findById(actorId).orElseThrow(() -> rule("Approval actor was not found"));
        actionRepository.save(ApprovalAction.builder().approvalRequest(r).action(type).actorUserId(actorId).actorNameSnapshot(actor.getName()).fromStatus(from).toStatus(to).comment(comment).build());
    }

    private User activeActor() {
        User user = userRepository.findById(currentUserService.getCurrentUserId()).orElseThrow(() -> rule("Approval actor was not found"));
        if (user.getStatus() != UserStatus.ACTIVE) throw rule("Only ACTIVE users can perform approval actions");
        return user;
    }

    private void authorizeRead(ApprovalRequest r) {
        Long id = currentUserService.getCurrentUserId();
        if (id.equals(r.getMakerUserId())) return;
        List<String> auth = authorities();
        if (auth.contains(VIEW_QUEUE) && auth.contains(r.getRequiredPermission())) return;
        throw rule("Approval request is not visible to the current user");
    }

    private List<String> authorities() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a == null ? List.of() : a.getAuthorities().stream().map(GrantedAuthority::getAuthority).distinct().toList();
    }

    private void requireAuthority(String value) {
        if (!authorities().contains(value)) throw rule("Required permission is missing: " + value);
    }

    private void requireMasterEnabled() {
        if (!properties.isEnabled()) throw rule("Approval workflow is disabled");
    }

    private void requireComment(ApprovalDecisionDto dto) {
        if (dto == null || clean(dto.getComment()) == null) throw rule("Decision comment is required");
    }

    private BusinessRuleException rule(String message) {
        return new BusinessRuleException(message);
    }

    private String clean(String value) {
        if (value == null) return null;
        String v = value.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", " ").trim();
        return v.isEmpty() ? null : v.substring(0, Math.min(500, v.length()));
    }

    private String cleanTitle(String value) {
        String v = clean(value);
        return v == null ? null : v.substring(0, Math.min(255, v.length()));
    }

    private ApprovalRequestResponseDto toResponse(ApprovalRequest r, boolean actions) {
        ApprovalDocumentAdapter adapter = adapter(r.getEntityType());
        String maker = userRepository.findById(r.getMakerUserId()).map(User::getName).orElse("Unknown user");
        String entityLabel = switch (r.getEntityType()) {
            case MANUAL_JOURNAL -> "Manual Journal";
            case VENDOR_BILL -> "Vendor Bill";
            case INVOICE -> "Invoice";
            case PAYMENT -> "Payment";
        };
        return ApprovalRequestResponseDto.builder().id(r.getId()).entityType(r.getEntityType()).entityId(r.getEntityId()).documentNumber(r.getDocumentNumber()).documentTitle(r.getDocumentTitle()).entityLabel(entityLabel).documentUrl(adapter.documentUrl(r.getEntityId())).makerUserId(r.getMakerUserId()).makerName(maker).status(r.getStatus()).requiredPermission(r.getRequiredPermission()).submittedAt(r.getSubmittedAt()).decidedAt(r.getDecidedAt()).decidedBy(r.getDecidedBy()).decisionComment(r.getDecisionComment()).consumedAt(r.getConsumedAt()).consumedBy(r.getConsumedBy()).supersedesRequestId(r.getSupersedesRequestId()).canDecide(r.getStatus() == ApprovalStatus.PENDING && !r.getMakerUserId().equals(currentUserService.getCurrentUserId()) && authorities().contains(r.getRequiredPermission())).actions(actions ? actionRepository.findByApprovalRequestIdOrderByCreatedAtAscIdAsc(r.getId()).stream().map(this::toAction).toList() : List.of()).build();
    }

    private ApprovalActionResponseDto toAction(ApprovalAction a) {
        return ApprovalActionResponseDto.builder().id(a.getId()).approvalRequestId(a.getApprovalRequest().getId()).action(a.getAction()).actorUserId(a.getActorUserId()).actorName(a.getActorNameSnapshot()).fromStatus(a.getFromStatus()).toStatus(a.getToStatus()).comment(a.getComment()).createdAt(a.getCreatedAt()).build();
    }
}
