package com.nexaerp.accountingperiod.checklist;

import com.nexaerp.accountingperiod.dto.PeriodCloseCheckResultDto;

import java.time.LocalDate;

public interface PeriodCloseCheck {

    String getCode();

    String getName();

    PeriodCloseCheckResultDto run(LocalDate periodEndDate);
}