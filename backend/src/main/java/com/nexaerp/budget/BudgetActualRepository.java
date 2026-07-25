package com.nexaerp.budget;

import com.nexaerp.journal.JournalLine;
import com.nexaerp.journal.JournalStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

// Extends the marker "Repository" (not JpaRepository) so it exposes ONLY this
// custom aggregation query — no CRUD methods duplicated on top of JournalLineRepository.
public interface BudgetActualRepository extends Repository<JournalLine, Long> {

    @Query("""
            SELECT jl.account.id AS accountId,
                   COALESCE(SUM(jl.debit), 0) AS totalDebit,
                   COALESCE(SUM(jl.credit), 0) AS totalCredit
            FROM JournalLine jl
            WHERE jl.journalEntry.status = :status
              AND jl.journalEntry.date BETWEEN :fromDate AND :toDate
              AND jl.account.id IN :accountIds
            GROUP BY jl.account.id
            """)
    List<AccountActualProjection> findAccountActuals(
            @Param("accountIds") List<Long> accountIds,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("status") JournalStatus status
    );
}