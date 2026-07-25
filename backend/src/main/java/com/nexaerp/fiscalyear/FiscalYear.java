package com.nexaerp.fiscalyear;

import com.nexaerp.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "fiscal_years",
        indexes = {
                @Index(name = "idx_fiscal_year_dates", columnList = "start_date,end_date"),
                @Index(name = "idx_fiscal_year_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FiscalYear extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private FiscalYearStatus status = FiscalYearStatus.DRAFT;

    @Column(length = 500)
    private String description;

    private LocalDateTime activatedAt;
    private Long activatedBy;

    private LocalDateTime closedAt;
    private Long closedBy;
}
