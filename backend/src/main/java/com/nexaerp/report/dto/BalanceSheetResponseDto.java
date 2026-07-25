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
public class BalanceSheetResponseDto {
    private LocalDate asOfDate;

    private List<BalanceSheetRowDto> assets;
    private BigDecimal totalAssets;

    private List<BalanceSheetRowDto> liabilities;
    private BigDecimal totalLiabilities;

    private List<BalanceSheetRowDto> equity;
    private BigDecimal totalEquityExcludingProfit;

    private BigDecimal netProfit; // current period profit added to equity
    private BigDecimal totalEquity; // totalEquityExcludingProfit + netProfit

    private BigDecimal totalLiabilitiesAndEquity; // should equal totalAssets
    private Boolean isBalanced;
}
