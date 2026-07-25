package com.nexaerp.accountingperiod;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AccountingPeriodRepository extends JpaRepository<AccountingPeriod, Long> {

    List<AccountingPeriod> findByFiscalYearIdAndDeletedAtIsNullOrderByPeriodNumberAsc(Long fiscalYearId);

    List<AccountingPeriod> findByDeletedAtIsNullOrderByStartDateDesc();

    Optional<AccountingPeriod> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByFiscalYearIdAndPeriodNumberAndDeletedAtIsNull(Long fiscalYearId, Integer periodNumber);

    boolean existsByFiscalYearIdAndPeriodNumberAndIdNotAndDeletedAtIsNull(
            Long fiscalYearId,
            Integer periodNumber,
            Long id
    );

    @Query("""
            select p from AccountingPeriod p
            where p.deletedAt is null
              and :date between p.startDate and p.endDate
            """)
    List<AccountingPeriod> findPeriodsContainingDate(@Param("date") LocalDate date);

    @Query("""
            select count(p) > 0 from AccountingPeriod p
            where p.deletedAt is null
              and p.fiscalYear.id = :fiscalYearId
              and (:excludeId is null or p.id <> :excludeId)
              and p.startDate <= :endDate
              and p.endDate >= :startDate
            """)
    boolean existsOverlappingPeriod(
            @Param("fiscalYearId") Long fiscalYearId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeId") Long excludeId
    );

    boolean existsByFiscalYearIdAndStatusAndDeletedAtIsNull(
            Long fiscalYearId,
            AccountingPeriodStatus status
    );
}
