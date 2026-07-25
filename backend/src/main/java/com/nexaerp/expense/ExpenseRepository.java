package com.nexaerp.expense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    Optional<Expense> findTopByOrderByIdDesc(); // for EXP-0001 number generation
    long countByStatus(ExpenseStatus status);

    @Query(
            "SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") ExpenseStatus status);

    @Query(
            "SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.status = :status AND e.expenseDate BETWEEN :fromDate AND :toDate")
    BigDecimal sumAmountByStatusAndDateBetween(
            @Param("status") ExpenseStatus status,
            @Param("fromDate") java.time.LocalDate fromDate,
            @Param("toDate") java.time.LocalDate toDate);

    @org.springframework.data.jpa.repository.Query(
            "SELECT COALESCE(SUM(e.dueAmount), 0) FROM Expense e WHERE e.status = com.nexaerp.expense.ExpenseStatus.POSTED AND e.paymentStatus <> com.nexaerp.expense.ExpensePaymentStatus.PAID")
    BigDecimal sumOutstandingDue();

    List<Expense> findByStatusAndExpenseDateLessThanEqual(ExpenseStatus status, LocalDate date);
}