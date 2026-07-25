package com.nexaerp.report.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CashFlowStatementResponseDto {
    private LocalDate fromDate;
    private LocalDate toDate;
    private String currencyCode;
    private BigDecimal openingCashBalance;
    private CashFlowActivitySectionDto operatingActivities;
    private BigDecimal netCashFromOperatingActivities;
    private CashFlowActivitySectionDto investingActivities;
    private BigDecimal netCashFromInvestingActivities;
    private CashFlowActivitySectionDto financingActivities;
    private BigDecimal netCashFromFinancingActivities;
    private BigDecimal netChangeInCash;
    private BigDecimal calculatedClosingCashBalance;
    private BigDecimal ledgerClosingCashBalance;
    private BigDecimal reconciliationDifference;
    private Boolean isReconciled;
    private List<CashFlowAccountBalanceDto> cashAccounts;
    private List<UnclassifiedCashMovementDto> unclassifiedMovements;
    private LocalDateTime generatedAt;
}
