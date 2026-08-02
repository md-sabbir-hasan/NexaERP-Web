package com.nexaerp.approval;

import com.nexaerp.approval.dto.*;
import com.nexaerp.common.response.PageResponseDto;

import java.util.List;

public interface ApprovalService {
    ApprovalRequestResponseDto submitManualJournal(Long journalId);
    ApprovalRequestResponseDto submitVendorBill(Long vendorBillId);
    ApprovalRequestResponseDto approveVendorBillCompatibility(Long vendorBillId);
    ApprovalRequestResponseDto approve(Long requestId, ApprovalDecisionDto decision);
    ApprovalRequestResponseDto reject(Long requestId, ApprovalDecisionDto decision);
    ApprovalRequestResponseDto returnForCorrection(Long requestId, ApprovalDecisionDto decision);
    PageResponseDto<ApprovalRequestResponseDto> pending(int page, int size);
    long pendingCount();
    PageResponseDto<ApprovalRequestResponseDto> myRequests(int page, int size);
    PageResponseDto<ApprovalActionResponseDto> myActions(int page, int size);
    ApprovalRequestResponseDto getById(Long id);
    List<ApprovalRequestResponseDto> history(ApprovalEntityType type, Long entityId);
    void assertJournalChangeAllowed(Long journalId);
    void assertVendorBillChangeAllowed(Long vendorBillId);
    ApprovalRequest lockAndValidateForPosting(Long journalId);
    ApprovalRequest lockAndValidateVendorBillForPosting(Long vendorBillId);
    ApprovalRequest lockActiveVendorBillForCancellation(Long vendorBillId);
    void cancelAfterSuccessfulDocumentCancellation(ApprovalRequest request);
    void consumeAfterSuccessfulPost(ApprovalRequest request);
    boolean isManualJournalApprovalEnabled();
    boolean isVendorBillApprovalEnabled();
    ApprovalRequest findLatestJournalRequest(Long journalId);
    ApprovalRequest findLatestVendorBillRequest(Long vendorBillId);
}
