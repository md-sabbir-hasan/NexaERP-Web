package com.nexaerp.report.dto;

import com.nexaerp.journal.JournalSourceType;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UnclassifiedCashMovementDto {
    private Long journalEntryId;
    private String entryNumber;
    private LocalDate date;
    private JournalSourceType sourceType;
    private Long sourceId;
    private String description;
    private BigDecimal amount;
    private String reason;
}
