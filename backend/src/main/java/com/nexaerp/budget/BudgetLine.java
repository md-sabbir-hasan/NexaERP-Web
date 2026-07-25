package com.nexaerp.budget;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nexaerp.account.Account;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "budget_lines",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_budget_line_account", columnNames = {"budget_id", "account_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal annualAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BudgetAllocationMethod allocationMethod;

    @Column(length = 300)
    private String notes;
}