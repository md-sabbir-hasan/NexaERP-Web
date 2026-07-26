package com.nexaerp.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessSummaryDto {
    private BigDecimal cashPosition;
    private Boolean cashConfigured;
    private LocalDate asOfDate;
    private String currencyCode;

    private BigDecimal accountsReceivable;    // total outstanding invoice due
    private Long overdueInvoiceCount;
    private BigDecimal overdueInvoiceAmount;

    private BigDecimal accountsPayable;       // total outstanding vendor bill due
    private Long overdueBillCount;
    private BigDecimal overdueBillAmount;

    private List<MonthlyTrendDto> revenueTrend;   // last 6 months
    private List<MonthlyTrendDto> expenseTrend;   // last 6 months
    private LocalDate trendFromDate;
    private LocalDate trendToDate;
}
