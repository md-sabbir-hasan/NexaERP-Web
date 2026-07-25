package com.nexaerp.budget;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetLineRepository extends JpaRepository<BudgetLine, Long> {

    List<BudgetLine> findByBudgetId(Long budgetId);

    Optional<BudgetLine> findByBudgetIdAndAccountId(Long budgetId, Long accountId);

    boolean existsByBudgetIdAndAccountId(Long budgetId, Long accountId);

    void deleteByBudgetId(Long budgetId);
}