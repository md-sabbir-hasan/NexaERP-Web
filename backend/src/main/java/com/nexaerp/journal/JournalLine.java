package com.nexaerp.journal;

import com.nexaerp.account.Account;
import com.nexaerp.costcenter.CostCenter;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "journal_lines", indexes = {
        @Index(name = "idx_journal_lines_cost_center", columnList = "cost_center_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntry journalEntry;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id")
    private CostCenter costCenter;

    @Column(precision = 19, scale = 2)
    private BigDecimal debit = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal credit = BigDecimal.ZERO;

    private String description;




}
