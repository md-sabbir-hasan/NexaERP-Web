package com.nexaerp.expense;

import com.nexaerp.account.Account;
import com.nexaerp.common.BaseEntity;
import com.nexaerp.party.Party;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "expenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String expenseNumber; // EXP-0001

    @Column(nullable = false)
    private LocalDate expenseDate;

    // Category — COA account of type EXPENSE (e.g. "Mobile Bill", "Utility")
    @ManyToOne
    @JoinColumn(name = "expense_account_id", nullable = false)
    private Account expenseAccount;

    // true = paid immediately (cash/bank/wallet), false = pay later (goes to payable)
    @Column(nullable = false)
    private Boolean paidImmediately;

    // Cash/Bank/Mobile-Wallet account money paid from. Required when paidImmediately = true
    @ManyToOne
    @JoinColumn(name = "payment_account_id")
    private Account paymentAccount;

    // Who the bill is paid/payable to. Required when paidImmediately = false
    @ManyToOne
    @JoinColumn(name = "party_id")
    private Party party;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(precision = 19, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal dueAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpensePaymentStatus paymentStatus;

    private String referenceNumber; // vendor/operator bill ref, optional

    private String attachmentUrl; // receipt photo — from existing /api/files/upload

    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseStatus status = ExpenseStatus.POSTED;

    private LocalDateTime cancelledAt;
    private Long cancelledBy;
    private String cancelReason;

    @Column(name = "recurring_template_id")
    private Long recurringTemplateId;
}