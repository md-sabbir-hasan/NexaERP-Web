package com.nexaerp.budget;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nexaerp.common.BaseEntity;
import com.nexaerp.fiscalyear.FiscalYear;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "budgets",
        indexes = {
                @Index(name = "idx_budget_fiscal_year", columnList = "fiscal_year_id"),
                @Index(name = "idx_budget_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Budget extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String budgetNumber; // BUD-2026-0001

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fiscal_year_id", nullable = false)
    private FiscalYear fiscalYear;

    @Column(nullable = false)
    @Builder.Default
    private Integer versionNumber = 1;

    // Future revision chain support — not exposed via any endpoint in MVP
    @Column(name = "revised_from_budget_id")
    private Long revisedFromBudgetId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BudgetStatus status = BudgetStatus.DRAFT;

    // Denormalized totals — kept in sync whenever lines change, for fast dashboard reads
    @Column(precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalRevenueBudget = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalExpenseBudget = BigDecimal.ZERO;

    private LocalDateTime activatedAt;
    private Long activatedBy;

    private LocalDateTime closedAt;
    private Long closedBy;
}