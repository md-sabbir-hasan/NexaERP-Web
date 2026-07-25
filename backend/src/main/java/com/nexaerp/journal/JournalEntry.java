package com.nexaerp.journal;

import com.nexaerp.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "journal_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalEntry extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String entryNumber; // JE-0001

    @Column(nullable = false)
    private LocalDate date;

    private String description;

    private String referenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JournalEntryType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JournalStatus status = JournalStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    private JournalSourceType sourceType = JournalSourceType.MANUAL;

    private Long sourceId; // Invoice ID / VendorBill ID

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    // Reverse reference
    private Long reversedFromId; // From which entry has it been reversed?

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JournalLine> lines;
}
