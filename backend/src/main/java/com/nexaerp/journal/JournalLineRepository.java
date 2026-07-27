package com.nexaerp.journal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface JournalLineRepository extends JpaRepository<JournalLine, Long> {
    List<JournalLine> findByJournalEntryId(Long journalEntryId);

    List<JournalLine> findByAccountId(Long accountId); //for Ledger

    @Query("SELECT l FROM JournalLine l " +
            "JOIN FETCH l.journalEntry e JOIN FETCH l.account a JOIN FETCH l.costCenter c " +
            "WHERE c.id = :costCenterId AND e.status IN :statuses " +
            "AND e.date BETWEEN :fromDate AND :toDate " +
            "ORDER BY e.date, e.id, l.id")
    List<JournalLine> findCostCenterTransactions(
            @Param("costCenterId") Long costCenterId,
            @Param("statuses") List<JournalStatus> statuses,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);


    // Used for Ledger report - all lines for an account within a date range
    List<JournalLine> findByAccountIdAndJournalEntry_DateBetweenOrderByJournalEntry_DateAsc(
            Long accountId, LocalDate fromDate, LocalDate toDate);

    // Used to calculate opening balance - all lines before fromDate
    List<JournalLine> findByAccountIdAndJournalEntry_DateBefore(
            Long accountId, LocalDate fromDate);

    boolean existsByAccountId(Long accountId);

    List<JournalLine> findByAccountIdAndJournalEntry_StatusInAndJournalEntry_DateBetweenOrderByJournalEntry_DateAsc(
            Long accountId,
            List<JournalStatus> statuses,
            LocalDate fromDate,
            LocalDate toDate
    );

    List<JournalLine> findByAccountIdAndJournalEntry_StatusInAndJournalEntry_DateBefore(
            Long accountId,
            List<JournalStatus> statuses,
            LocalDate fromDate
    );

    @Query("SELECT COALESCE(SUM(l.debit - l.credit), 0) FROM JournalLine l " +
            "WHERE l.account.id IN :accountIds AND l.journalEntry.status IN :statuses " +
            "AND l.journalEntry.date < :date")
    BigDecimal sumCashEffectBefore(@Param("accountIds") List<Long> accountIds,
                                   @Param("statuses") List<JournalStatus> statuses,
                                   @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(l.debit - l.credit), 0) FROM JournalLine l " +
            "WHERE l.account.id IN :accountIds AND l.journalEntry.status IN :statuses " +
            "AND l.journalEntry.date <= :date")
    BigDecimal sumCashEffectThrough(@Param("accountIds") List<Long> accountIds,
                                    @Param("statuses") List<JournalStatus> statuses,
                                    @Param("date") LocalDate date);

    @Query("SELECT DISTINCT l.journalEntry.id FROM JournalLine l " +
            "WHERE l.account.id IN :accountIds AND l.journalEntry.status IN :statuses " +
            "AND l.journalEntry.date BETWEEN :fromDate AND :toDate")
    List<Long> findJournalIdsContainingCash(@Param("accountIds") List<Long> accountIds,
                                            @Param("statuses") List<JournalStatus> statuses,
                                            @Param("fromDate") LocalDate fromDate,
                                            @Param("toDate") LocalDate toDate);

    @Query("SELECT l FROM JournalLine l JOIN FETCH l.account JOIN FETCH l.journalEntry " +
            "WHERE l.journalEntry.id IN :journalIds " +
            "ORDER BY l.journalEntry.date, l.journalEntry.id, l.id")
    List<JournalLine> findAllForJournalIds(@Param("journalIds") List<Long> journalIds);

    @Query("SELECT l.account.id, " +
            "COALESCE(SUM(CASE WHEN l.journalEntry.date < :fromDate THEN l.debit - l.credit ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN l.journalEntry.date BETWEEN :fromDate AND :toDate THEN l.debit - l.credit ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN l.journalEntry.date <= :toDate THEN l.debit - l.credit ELSE 0 END), 0) " +
            "FROM JournalLine l WHERE l.account.id IN :accountIds " +
            "AND l.journalEntry.status IN :statuses AND l.journalEntry.date <= :toDate GROUP BY l.account.id")
    List<Object[]> aggregateCashAccountBalances(@Param("accountIds") List<Long> accountIds,
                                                @Param("statuses") List<JournalStatus> statuses,
                                                @Param("fromDate") LocalDate fromDate,
                                                @Param("toDate") LocalDate toDate);


}
