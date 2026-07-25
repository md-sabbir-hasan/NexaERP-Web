package com.nexaerp.report.dto;

import com.nexaerp.journal.JournalSourceType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntryDto {
    private Long journalEntryId;
    private LocalDate date;
    private String journalEntryNumber;
    private JournalSourceType sourceType;
    private Long sourceId;
    private String referenceNumber;
    private String description;
    private BigDecimal debit;
    private BigDecimal credit;
    private BigDecimal runningBalance;
}
