package com.nexaerp.fiscalyear;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FiscalYearRepository extends JpaRepository<FiscalYear, Long> {

    List<FiscalYear> findByDeletedAtIsNullOrderByStartDateDesc();

    Optional<FiscalYear> findByIdAndDeletedAtIsNull(Long id);

    Optional<FiscalYear> findFirstByStatusAndDeletedAtIsNull(FiscalYearStatus status);

    boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);

    boolean existsByNameIgnoreCaseAndIdNotAndDeletedAtIsNull(String name, Long id);

    @Query("""
            select count(fy) > 0
            from FiscalYear fy
            where fy.deletedAt is null
              and fy.startDate <= :endDate
              and fy.endDate >= :startDate
            """)
    boolean existsOverlappingPeriod(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            select count(fy) > 0
            from FiscalYear fy
            where fy.deletedAt is null
              and fy.id <> :id
              and fy.startDate <= :endDate
              and fy.endDate >= :startDate
            """)
    boolean existsOverlappingPeriodExcludingId(
            @Param("id") Long id,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            select fy
            from FiscalYear fy
            where fy.deletedAt is null
              and :date between fy.startDate and fy.endDate
            order by fy.startDate desc
            """)
    List<FiscalYear> findContainingDate(@Param("date") LocalDate date);
}
