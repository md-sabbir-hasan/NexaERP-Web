package com.nexaerp.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostCenterTransactionReportDto {
    private Long costCenterId;
    private String costCenterCode;
    private String costCenterName;
    private LocalDate fromDate;
    private LocalDate toDate;
    private List<CostCenterTransactionLineDto> rows;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private BigDecimal netAmount;
}
