package com.nexaerp.report.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CashFlowAccountBalanceDto {
    private Long accountId;
    private String accountCode;
    private String accountName;
    private BigDecimal openingBalance;
    private BigDecimal periodMovement;
    private BigDecimal closingBalance;
}
