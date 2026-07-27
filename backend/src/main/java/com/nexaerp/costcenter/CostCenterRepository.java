package com.nexaerp.costcenter;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CostCenterRepository extends JpaRepository<CostCenter, Long> {
    boolean existsByCodeIgnoreCase(String code);
    Optional<CostCenter> findByIdAndDeletedAtIsNull(Long id);
    List<CostCenter> findByDeletedAtIsNullOrderByCodeAsc();
    List<CostCenter> findByIsActiveTrueAndDeletedAtIsNullOrderByCodeAsc();
    List<CostCenter> findByIsActiveAndDeletedAtIsNullOrderByCodeAsc(Boolean active);
    List<CostCenter> findByCodeContainingIgnoreCaseOrNameContainingIgnoreCaseOrderByCodeAsc(
            String code, String name);
    List<CostCenter> findByIsActiveAndCodeContainingIgnoreCaseOrIsActiveAndNameContainingIgnoreCaseOrderByCodeAsc(
            Boolean active1, String code, Boolean active2, String name);
}
