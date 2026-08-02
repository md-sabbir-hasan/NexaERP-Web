package com.nexaerp.approval;

import com.nexaerp.approval.dto.*;
import com.nexaerp.common.response.PageResponseDto;

import java.util.List;

public interface ApprovalService {
    ApprovalRequestResponseDto submitManualJournal(Long journalId);
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
    ApprovalRequest lockAndValidateForPosting(Long journalId);
    void consumeAfterSuccessfulPost(ApprovalRequest request);
    boolean isManualJournalApprovalEnabled();
    ApprovalRequest findLatestJournalRequest(Long journalId);
}
