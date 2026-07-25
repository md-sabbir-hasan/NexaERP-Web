package com.nexaerp.journal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface JournalLineRepository extends JpaRepository<JournalLine, Long> {
    List<JournalLine> findByJournalEntryId(Long journalEntryId);

    List<JournalLine> findByAccountId(Long accountId); //for Ledger


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


}
