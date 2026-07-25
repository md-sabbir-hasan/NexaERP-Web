package com.nexaerp.report.dto;

import com.nexaerp.report.CashFlowLineItem;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CashFlowLineItemDto {
    private CashFlowLineItem lineItem;
    private String label;
    private BigDecimal inflow;
    private BigDecimal outflow;
    private BigDecimal netAmount;
}
