package com.nexaerp.accountingperiod;

import com.nexaerp.accountingperiod.dto.AccountingPeriodRequestDto;
import com.nexaerp.accountingperiod.dto.AccountingPeriodResponseDto;
import com.nexaerp.accountingperiod.dto.PeriodCloseChecklistResponseDto;

import java.time.LocalDate;
import java.util.List;

public interface AccountingPeriodService {

    AccountingPeriodResponseDto create(AccountingPeriodRequestDto request);

    List<AccountingPeriodResponseDto> generateMonthlyPeriods(Long fiscalYearId);

    AccountingPeriodResponseDto update(Long id, AccountingPeriodRequestDto request);

    AccountingPeriodResponseDto getById(Long id);

    List<AccountingPeriodResponseDto> getAll(Long fiscalYearId);

    AccountingPeriodResponseDto getCurrent(LocalDate date);

    AccountingPeriodResponseDto open(Long id, String remarks);

    AccountingPeriodResponseDto close(Long id, String remarks);

    AccountingPeriodResponseDto lock(Long id, String remarks);

    PeriodCloseChecklistResponseDto getCloseChecklist(Long id);

    void delete(Long id);

    void validatePostingDate(LocalDate postingDate);
}