package com.nexaerp.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessSummaryDto {
    private BigDecimal cashPosition;          // total balance across active bank/cash/wallet accounts

    private BigDecimal accountsReceivable;    // total outstanding invoice due
    private long overdueInvoiceCount;
    private BigDecimal overdueInvoiceAmount;

    private BigDecimal accountsPayable;       // total outstanding vendor bill due
    private long overdueBillCount;
    private BigDecimal overdueBillAmount;

    private List<MonthlyTrendDto> revenueTrend;   // last 6 months
    private List<MonthlyTrendDto> expenseTrend;   // last 6 months
}