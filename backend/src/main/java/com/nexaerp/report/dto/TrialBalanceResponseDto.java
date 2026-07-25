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
public class TrialBalanceResponseDto {
    private LocalDate asOfDate;
    private List<TrialBalanceRowDto> rows;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private Boolean isBalanced;
}
