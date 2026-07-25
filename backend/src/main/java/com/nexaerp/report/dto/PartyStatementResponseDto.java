package com.nexaerp.report.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartyStatementResponseDto {
    private Long partyId;
    private String partyName;
    private String partyType;
    private LocalDate fromDate;
    private LocalDate toDate;
    private BigDecimal openingBalance;
    private List<PartyStatementEntryDto> entries;
    private BigDecimal closingBalance;
}
