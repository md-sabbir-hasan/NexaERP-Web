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
public class AgingResponseDto {
    private LocalDate asOfDate;
    private String partyType; // CUSTOMER or VENDOR
    private List<AgingRowDto> rows;
    private BigDecimal totalDue;
}
