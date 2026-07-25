package com.nexaerp.journal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    Optional<JournalEntry> findTopByOrderByIdDesc(); //For Last entry number
    boolean existsBySourceTypeAndSourceId(JournalSourceType sourceType, Long sourceId);
    Optional<JournalEntry> findBySourceTypeAndSourceId(JournalSourceType sourceType, Long sourceId);
    long countByStatus(JournalStatus status);
    List<JournalEntry> findByStatusAndDateLessThanEqual(JournalStatus status, LocalDate date);
}
