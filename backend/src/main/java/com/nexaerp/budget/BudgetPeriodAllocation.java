package com.nexaerp.budget;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nexaerp.accountingperiod.AccountingPeriod;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "budget_period_allocations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_budget_period_line", columnNames = {"budget_line_id", "accounting_period_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetPeriodAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_line_id", nullable = false)
    private BudgetLine budgetLine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "accounting_period_id", nullable = false)
    private AccountingPeriod accountingPeriod;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal budgetAmount;
}