package com.nexaerp.dashboard;

import com.nexaerp.account.AccountType;
import com.nexaerp.journal.JournalLine;
import com.nexaerp.journal.JournalStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;

// Marker "Repository" (not JpaRepository) - exposes only this aggregation query,
// no CRUD duplicated on top of JournalLineRepository. Same pattern as BudgetActualRepository.
public interface DashboardFinanceRepository extends Repository<JournalLine, Long> {

    // Returns (SUM(debit) - SUM(credit)) for the given account type within the date range.
    // For EXPENSE accounts this IS the natural-balance actual. For REVENUE accounts,
    // negate() the result to get the natural-balance actual (Credit - Debit).
    @Query("""
            SELECT COALESCE(SUM(jl.debit), 0) - COALESCE(SUM(jl.credit), 0)
            FROM JournalLine jl
            WHERE jl.journalEntry.status = :status
              AND jl.journalEntry.date BETWEEN :fromDate AND :toDate
              AND jl.account.type = :accountType
            """)
    BigDecimal sumNetDebitBetween(
            @Param("accountType") AccountType accountType,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("status") JournalStatus status
    );
}