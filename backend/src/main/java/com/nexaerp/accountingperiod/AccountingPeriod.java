package com.nexaerp.accountingperiod;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nexaerp.fiscalyear.FiscalYear;
import com.nexaerp.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "accounting_periods",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_accounting_period_fiscal_year_number",
                        columnNames = {"fiscal_year_id", "period_number"}
                )
        },
        indexes = {
                @Index(name = "idx_accounting_period_dates", columnList = "start_date,end_date"),
                @Index(name = "idx_accounting_period_status", columnList = "status"),
                @Index(name = "idx_accounting_period_fiscal_year", columnList = "fiscal_year_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountingPeriod extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fiscal_year_id", nullable = false)
    private FiscalYear fiscalYear;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "period_number", nullable = false)
    private Integer periodNumber;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AccountingPeriodStatus status = AccountingPeriodStatus.OPEN;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "closed_by")
    private Long closedBy;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "locked_by")
    private Long lockedBy;

    @Column(length = 500)
    private String remarks;
}
