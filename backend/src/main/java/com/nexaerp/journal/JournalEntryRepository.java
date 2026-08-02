package com.nexaerp.journal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from JournalEntry j where j.id = :id")
    Optional<JournalEntry> findByIdForUpdate(@Param("id") Long id);
    Optional<JournalEntry> findTopByOrderByIdDesc(); //For Last entry number
    boolean existsBySourceTypeAndSourceId(JournalSourceType sourceType, Long sourceId);
    Optional<JournalEntry> findBySourceTypeAndSourceId(JournalSourceType sourceType, Long sourceId);
    long countByStatus(JournalStatus status);
    List<JournalEntry> findByStatusAndDateLessThanEqual(JournalStatus status, LocalDate date);
    Page<JournalEntry> findByEntryNumberContainingIgnoreCase(String entryNumber, Pageable pageable);
}
