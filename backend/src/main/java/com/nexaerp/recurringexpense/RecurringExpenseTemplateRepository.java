package com.nexaerp.recurringexpense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecurringExpenseTemplateRepository extends JpaRepository<RecurringExpenseTemplate, Long> {

    Optional<RecurringExpenseTemplate> findByIdAndDeletedAtIsNull(Long id);

    List<RecurringExpenseTemplate> findByDeletedAtIsNullOrderByCreatedAtDesc();

    List<RecurringExpenseTemplate> findByStatusAndNextRunDateLessThanEqualAndDeletedAtIsNull(
            RecurringExpenseStatus status, LocalDate date);

    long countByStatus(RecurringExpenseStatus status);

    long countByStatusAndNextRunDateLessThanEqualAndDeletedAtIsNull(RecurringExpenseStatus status, LocalDate date);
}