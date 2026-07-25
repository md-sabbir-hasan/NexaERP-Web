package com.nexaerp.payment;

import com.nexaerp.account.Account;
import com.nexaerp.common.BaseEntity;
import com.nexaerp.party.Party;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String paymentNumber; // PAY-2025-000001

    @Column(nullable = false)
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType paymentType; // RECEIPT or PAYMENT

    @ManyToOne
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

    // The Cash/Bank/bKash account money goes into or out of
    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    // How much of amount allocated to invoices/bills
    @Column(precision = 19, scale = 2)
    private BigDecimal allocatedAmount = BigDecimal.ZERO;

    // Remaining amount not yet allocated
    @Column(precision = 19, scale = 2)
    private BigDecimal unallocatedAmount = BigDecimal.ZERO;

    private String currencyCode = "BDT";
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    private String transactionRef; // bKash TrxID, Cheque No, etc.
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.DRAFT;

    // Workflow
    private LocalDateTime postedAt;
    private Long postedBy;

    // Audit
//    private Long createdBy;
//    private LocalDateTime createdAt;
//    private Long updatedBy;
//    private LocalDateTime updatedAt;


    private LocalDateTime cancelledAt;
    private Long cancelledBy;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentAllocation> allocations;

}
