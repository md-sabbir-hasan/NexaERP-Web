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
public class PeriodCloseChecklistResponseDto {
    private Long periodId;
    private String periodName;
    private boolean allPassed;
    private List<PeriodCloseCheckResultDto> checks;
}