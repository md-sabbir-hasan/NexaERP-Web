package com.nexaerp.accountingperiod.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeriodCloseCheckResultDto {
    private String code;
    private String name;
    private boolean passed;
    private int count;
    private List<PeriodCloseCheckItemDto> items;
}