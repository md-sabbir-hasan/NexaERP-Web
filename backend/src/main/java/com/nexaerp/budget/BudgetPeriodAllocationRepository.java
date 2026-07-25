package com.nexaerp.budget;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetPeriodAllocationRepository extends JpaRepository<BudgetPeriodAllocation, Long> {

    List<BudgetPeriodAllocation> findByBudgetLineId(Long budgetLineId);

    List<BudgetPeriodAllocation> findByBudgetLineIdIn(List<Long> budgetLineIds);

    Optional<BudgetPeriodAllocation> findByBudgetLineIdAndAccountingPeriodId(Long budgetLineId, Long accountingPeriodId);

    void deleteByBudgetLineId(Long budgetLineId);
}