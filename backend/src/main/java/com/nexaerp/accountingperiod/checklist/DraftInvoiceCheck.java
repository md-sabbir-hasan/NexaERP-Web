package com.nexaerp.accountingperiod.checklist;

import com.nexaerp.accountingperiod.dto.PeriodCloseCheckItemDto;
import com.nexaerp.accountingperiod.dto.PeriodCloseCheckResultDto;
import com.nexaerp.invoice.Invoice;
import com.nexaerp.invoice.InvoiceRepository;
import com.nexaerp.invoice.InvoiceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DraftInvoiceCheck implements PeriodCloseCheck {

    private final InvoiceRepository invoiceRepository;

    @Override
    public String getCode() {
        return "DRAFT_INVOICE";
    }

    @Override
    public String getName() {
        return "Draft Invoice";
    }

    @Override
    public PeriodCloseCheckResultDto run(LocalDate periodEndDate) {
        List<Invoice> drafts =
                invoiceRepository.findByStatusAndInvoiceDateLessThanEqual(InvoiceStatus.DRAFT, periodEndDate);

        List<PeriodCloseCheckItemDto> items = drafts.stream()
                .map(i -> PeriodCloseCheckItemDto.builder()
                        .id(i.getId())
                        .reference(i.getInvoiceNumber())
                        .date(i.getInvoiceDate())
                        .amount(i.getGrandTotal())
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