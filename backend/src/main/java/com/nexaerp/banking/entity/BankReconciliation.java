package com.nexaerp.banking.entity;


import com.nexaerp.banking.enums.ReconciliationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_reconciliations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankReconciliation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bank_account_id", nullable = false)
    private BankAccount bankAccount;

    // Statement end date, as printed on the bank statement
    @Column(nullable = false)
    private LocalDate statementDate;

    // Closing balance as shown on the bank statement (entered manually by user)
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal statementBalance;

    // System book balance as of statementDate, snapshotted when reconciliation is started
    @Column(precision = 19, scale = 2)
    private BigDecimal bookBalance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReconciliationStatus status = ReconciliationStatus.IN_PROGRESS;

    private String notes;

    private LocalDateTime completedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
