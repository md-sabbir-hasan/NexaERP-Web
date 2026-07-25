package com.nexaerp.report.dto;

import com.nexaerp.report.CashFlowActivity;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CashFlowActivitySectionDto {
    private CashFlowActivity activity;
    private List<CashFlowLineItemDto> items;
    private BigDecimal totalInflows;
    private BigDecimal totalOutflows;
    private BigDecimal netCashFlow;
}
