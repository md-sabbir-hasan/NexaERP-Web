package com.nexaerp.approval;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {
    Optional<ApprovalRequest> findByEntityTypeAndEntityIdAndActiveMarker(ApprovalEntityType type, Long entityId, Integer marker);
    Optional<ApprovalRequest> findTopByEntityTypeAndEntityIdOrderBySubmittedAtDesc(ApprovalEntityType type, Long entityId);
    List<ApprovalRequest> findByEntityTypeAndEntityIdOrderBySubmittedAtDesc(ApprovalEntityType type, Long entityId);
    Page<ApprovalRequest> findByMakerUserIdOrderBySubmittedAtDesc(Long makerUserId, Pageable pageable);

    @Query("select r from ApprovalRequest r where r.status = com.nexaerp.approval.ApprovalStatus.PENDING " +
            "and r.requiredPermission in :permissions and r.makerUserId <> :userId order by r.submittedAt asc")
    Page<ApprovalRequest> findPendingForUser(@Param("userId") Long userId, @Param("permissions") List<String> permissions, Pageable pageable);

    @Query("select count(r) from ApprovalRequest r where r.status = com.nexaerp.approval.ApprovalStatus.PENDING " +
            "and r.requiredPermission in :permissions and r.makerUserId <> :userId")
    long countPendingForUser(@Param("userId") Long userId, @Param("permissions") List<String> permissions);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ApprovalRequest r where r.id = :id")
    Optional<ApprovalRequest> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ApprovalRequest r where r.entityType = :type and r.entityId = :entityId and r.activeMarker = 1")
    Optional<ApprovalRequest> findActiveForUpdate(@Param("type") ApprovalEntityType type, @Param("entityId") Long entityId);
}
