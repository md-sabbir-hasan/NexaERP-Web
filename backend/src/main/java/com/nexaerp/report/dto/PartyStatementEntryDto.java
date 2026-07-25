package com.nexaerp.report.dto;

import com.nexaerp.report.StatementEntryType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartyStatementEntryDto {
    private LocalDate date;
    private StatementEntryType type;
    private Long referenceId;
    private String referenceNumber;
    private String description;
    private BigDecimal debit;
    private BigDecimal credit;
    private BigDecimal runningBalance;
}
