package com.nexaerp.approval;

import com.nexaerp.approval.dto.*;
import com.nexaerp.common.response.PageResponseDto;

import java.util.List;

public interface ApprovalService {
    ApprovalRequestResponseDto submitManualJournal(Long journalId);
    ApprovalRequestResponseDto submitVendorBill(Long vendorBillId);
    ApprovalRequestResponseDto submitInvoice(Long invoiceId);
    ApprovalRequestResponseDto submitPayment(Long paymentId);
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
    void assertInvoiceChangeAllowed(Long invoiceId);
    void assertPaymentChangeAllowed(Long paymentId);
    ApprovalRequest lockAndValidateForPosting(Long journalId);
    ApprovalRequest lockAndValidateVendorBillForPosting(Long vendorBillId);
    ApprovalRequest lockAndValidateInvoiceForPosting(Long invoiceId);
    ApprovalRequest lockAndValidatePaymentForPosting(Long paymentId);
    ApprovalRequest lockActiveVendorBillForCancellation(Long vendorBillId);
    ApprovalRequest lockActiveInvoiceForCancellation(Long invoiceId);
    ApprovalRequest lockActivePaymentForCancellation(Long paymentId);
    void cancelAfterSuccessfulDocumentCancellation(ApprovalRequest request);
    void consumeAfterSuccessfulPost(ApprovalRequest request);
    boolean isManualJournalApprovalEnabled();
    boolean isVendorBillApprovalEnabled();
    boolean isInvoiceApprovalEnabled();
    boolean isPaymentApprovalEnabled();
    ApprovalRequest findLatestJournalRequest(Long journalId);
    ApprovalRequest findLatestVendorBillRequest(Long vendorBillId);
    ApprovalRequest findLatestInvoiceRequest(Long invoiceId);
    ApprovalRequest findLatestPaymentRequest(Long paymentId);
}
