package com.nexaerp.fiscalyear;

import com.nexaerp.fiscalyear.dto.FiscalYearRequestDto;
import com.nexaerp.fiscalyear.dto.FiscalYearResponseDto;

import java.time.LocalDate;
import java.util.List;

public interface FiscalYearService {
    FiscalYearResponseDto create(FiscalYearRequestDto request);
    FiscalYearResponseDto update(Long id, FiscalYearRequestDto request);
    FiscalYearResponseDto getById(Long id);
    List<FiscalYearResponseDto> getAll();
    FiscalYearResponseDto getActive();
    FiscalYearResponseDto getByDate(LocalDate date);
    FiscalYearResponseDto activate(Long id);
    FiscalYearResponseDto close(Long id);
    void delete(Long id);
}
