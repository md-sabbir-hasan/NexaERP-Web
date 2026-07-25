package com.nexaerp.banking.entity;


import com.nexaerp.banking.enums.TransactionSourceType;
import com.nexaerp.banking.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String transactionNumber; // TXN-2025-000001

    @ManyToOne
    @JoinColumn(name = "bank_account_id", nullable = false)
    private BankAccount bankAccount;

    @Column(nullable = false)
    private LocalDate transactionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType; // CREDIT / DEBIT

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    private String description;
    private String referenceNumber; // Cheque no, TrxID etc.

    // Which COA account is the other side of this transaction
    private Long contraAccountId;

    private Boolean reconciled = false;
    private LocalDateTime reconciledAt;

    // Which BankReconciliation batch matched this transaction (nullable)
    private Long reconciliationId;

    private Boolean voided = false;
    private LocalDateTime voidedAt;

    @Enumerated(EnumType.STRING)
    private TransactionSourceType sourceType = TransactionSourceType.MANUAL;

    private Long sourceId;

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
