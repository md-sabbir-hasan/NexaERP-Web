package com.nexaerp.accountingperiod.checklist;

import com.nexaerp.accountingperiod.dto.PeriodCloseCheckItemDto;
import com.nexaerp.accountingperiod.dto.PeriodCloseCheckResultDto;
import com.nexaerp.payment.Payment;
import com.nexaerp.payment.PaymentRepository;
import com.nexaerp.payment.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DraftPaymentCheck implements PeriodCloseCheck {

    private final PaymentRepository paymentRepository;

    @Override
    public String getCode() {
        return "DRAFT_PAYMENT";
    }

    @Override
    public String getName() {
        return "Draft Payment";
    }

    @Override
    public PeriodCloseCheckResultDto run(LocalDate periodEndDate) {
        List<Payment> drafts =
                paymentRepository.findByStatusAndPaymentDateLessThanEqual(PaymentStatus.DRAFT, periodEndDate);

        List<PeriodCloseCheckItemDto> items = drafts.stream()
                .map(p -> PeriodCloseCheckItemDto.builder()
                        .id(p.getId())
                        .reference(p.getPaymentNumber())
                        .date(p.getPaymentDate())
                        .amount(p.getAmount())
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