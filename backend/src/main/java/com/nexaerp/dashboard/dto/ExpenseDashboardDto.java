package com.nexaerp.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseDashboardDto {
    private long draftCount;              // pending review — awaiting "Post"
    private BigDecimal draftTotalAmount;
    private BigDecimal postedThisMonthTotal;
    private long recurringActiveCount;
    private long recurringDueSoonCount;   // nextRunDate within next 7 days
}