package com.nexaerp.accountingperiod;

import com.nexaerp.accountingperiod.checklist.PeriodCloseCheck;
import com.nexaerp.accountingperiod.dto.PeriodCloseChecklistResponseDto;
import com.nexaerp.accountingperiod.dto.PeriodCloseCheckResultDto;
import com.nexaerp.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PeriodCloseValidationService {

    private final AccountingPeriodRepository accountingPeriodRepository;
    private final List<PeriodCloseCheck> checks; // Spring injects all @Component beans implementing this interface

    @Transactional(readOnly = true)
    public PeriodCloseChecklistResponseDto runChecklist(Long periodId) {
        AccountingPeriod period = accountingPeriodRepository.findByIdAndDeletedAtIsNull(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("Accounting period not found"));

        List<PeriodCloseCheckResultDto> results = checks.stream()
                .map(check -> check.run(period.getEndDate()))
                .toList();

        boolean allPassed = results.stream().allMatch(PeriodCloseCheckResultDto::isPassed);

        return PeriodCloseChecklistResponseDto.builder()
                .periodId(period.getId())
                .periodName(period.getName())
                .allPassed(allPassed)
                .checks(results)
                .build();
    }
}