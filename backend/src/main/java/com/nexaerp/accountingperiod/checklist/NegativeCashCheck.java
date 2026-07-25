package com.nexaerp.accountingperiod.checklist;

import com.nexaerp.accountingperiod.dto.PeriodCloseCheckItemDto;
import com.nexaerp.accountingperiod.dto.PeriodCloseCheckResultDto;
import com.nexaerp.banking.entity.BankAccount;
import com.nexaerp.banking.repository.BankAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class NegativeCashCheck implements PeriodCloseCheck {

    private final BankAccountRepository bankAccountRepository;

    @Override
    public String getCode() {
        return "NEGATIVE_CASH";
    }

    @Override
    public String getName() {
        return "Negative Cash";
    }

    @Override
    public PeriodCloseCheckResultDto run(LocalDate periodEndDate) {
        // currentBalance is a live running balance, not a point-in-time snapshot —
        // this check reflects the account state as of now, not strictly as of periodEndDate.
        List<BankAccount> negatives =
                bankAccountRepository.findByIsActiveTrueAndCurrentBalanceLessThan(BigDecimal.ZERO);

        List<PeriodCloseCheckItemDto> items = negatives.stream()
                .map(a -> PeriodCloseCheckItemDto.builder()
                        .id(a.getId())
                        .reference(a.getAccountName())
                        .amount(a.getCurrentBalance())
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