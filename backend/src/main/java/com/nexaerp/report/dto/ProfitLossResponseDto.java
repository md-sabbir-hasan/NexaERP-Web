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
public class ProfitLossResponseDto {
    private LocalDate fromDate;
    private LocalDate toDate;

    private List<ProfitLossRowDto> revenues;
    private BigDecimal totalRevenue;

    private List<ProfitLossRowDto> expenses;
    private BigDecimal totalExpense;

    private BigDecimal netProfit; // totalRevenue - totalExpense
}
