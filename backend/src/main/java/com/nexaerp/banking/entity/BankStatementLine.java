package com.nexaerp.banking.entity;


import com.nexaerp.banking.enums.StatementLineStatus;
import com.nexaerp.banking.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_statement_lines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankStatementLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "reconciliation_id", nullable = false)
    private BankReconciliation reconciliation;

    @Column(nullable = false)
    private LocalDate lineDate;

    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;

    private String referenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatementLineStatus status = StatementLineStatus.UNMATCHED;

    // The existing BankTransaction this line was matched against (auto or manual)
    private Long matchedTransactionId;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
