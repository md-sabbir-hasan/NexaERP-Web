package com.nexaerp.budget;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByIdAndDeletedAtIsNull(Long id);

    List<Budget> findByDeletedAtIsNullOrderByCreatedAtDesc();

    List<Budget> findByFiscalYearIdAndDeletedAtIsNullOrderByVersionNumberDesc(Long fiscalYearId);

    Optional<Budget> findByFiscalYearIdAndStatusAndDeletedAtIsNull(Long fiscalYearId, BudgetStatus status);

    boolean existsByFiscalYearIdAndStatusAndDeletedAtIsNull(Long fiscalYearId, BudgetStatus status);

    Optional<Budget> findTopByOrderByIdDesc(); // for budgetNumber generation
}