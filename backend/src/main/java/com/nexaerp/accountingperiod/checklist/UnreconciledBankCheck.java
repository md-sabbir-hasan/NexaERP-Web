package com.nexaerp.accountingperiod.checklist;

import com.nexaerp.accountingperiod.dto.PeriodCloseCheckItemDto;
import com.nexaerp.accountingperiod.dto.PeriodCloseCheckResultDto;
import com.nexaerp.banking.entity.BankTransaction;
import com.nexaerp.banking.repository.BankTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UnreconciledBankCheck implements PeriodCloseCheck {

    private final BankTransactionRepository bankTransactionRepository;

    @Override
    public String getCode() {
        return "UNRECONCILED_BANK";
    }

    @Override
    public String getName() {
        return "Unreconciled Bank";
    }

    @Override
    public PeriodCloseCheckResultDto run(LocalDate periodEndDate) {
        List<BankTransaction> unreconciled = bankTransactionRepository
                .findByReconciledFalseAndVoidedFalseAndTransactionDateLessThanEqual(periodEndDate);

        List<PeriodCloseCheckItemDto> items = unreconciled.stream()
                .map(t -> PeriodCloseCheckItemDto.builder()
                        .id(t.getId())
                        .reference(t.getTransactionNumber())
                        .date(t.getTransactionDate())
                        .amount(t.getAmount())
                        .build())
                .toList();

        return PeriodCloseCheckResultDto.builder()
                .code(getCode())
                .name(getName())
                .passed(items.isEmpty())
                .count(items.size())
                .items(items)
                .build();
    }
}