package com.nexaerp.report.dto;

import com.nexaerp.journal.JournalSourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostCenterTransactionLineDto {
    private Long journalEntryId;
    private String journalNumber;
    private LocalDate date;
    private JournalSourceType source;
    private Long sourceId;
    private String accountCode;
    private String accountName;
    private BigDecimal debit;
    private BigDecimal credit;
    private String description;
}
