package com.nexaerp.recurringexpense;

import com.nexaerp.account.Account;
import com.nexaerp.common.BaseEntity;
import com.nexaerp.party.Party;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "recurring_expense_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringExpenseTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_account_id", nullable = false)
    private Account expenseAccount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private Boolean paidImmediately;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_account_id")
    private Account paymentAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id")
    private Party party;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecurringFrequency frequency;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate nextRunDate;

    private LocalDate endDate;

    private LocalDate lastGeneratedDate;

    private Long lastGeneratedExpenseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RecurringExpenseStatus status = RecurringExpenseStatus.ACTIVE;

    private String referenceNumber;

    @Column(length = 300)
    private String notes;

    @Column(length = 500)
    private String lastRunError; // populated if a generation attempt fails, cleared on next success
}