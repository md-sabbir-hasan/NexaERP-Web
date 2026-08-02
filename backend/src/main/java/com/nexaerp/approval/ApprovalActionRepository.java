package com.nexaerp.approval;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalActionRepository extends JpaRepository<ApprovalAction, Long> {
    List<ApprovalAction> findByApprovalRequestIdOrderByCreatedAtAscIdAsc(Long requestId);
    Page<ApprovalAction> findByActorUserIdOrderByCreatedAtDesc(Long actorUserId, Pageable pageable);
}
