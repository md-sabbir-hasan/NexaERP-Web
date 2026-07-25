package com.nexaerp.accountingperiod.checklist;

import com.nexaerp.accountingperiod.dto.PeriodCloseCheckItemDto;
import com.nexaerp.accountingperiod.dto.PeriodCloseCheckResultDto;
import com.nexaerp.vendorbill.VendorBill;
import com.nexaerp.vendorbill.VendorBillRepository;
import com.nexaerp.vendorbill.VendorBillStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DraftVendorBillCheck implements PeriodCloseCheck {

    private final VendorBillRepository vendorBillRepository;

    @Override
    public String getCode() {
        return "DRAFT_VENDOR_BILL";
    }

    @Override
    public String getName() {
        return "Draft Vendor Bill";
    }

    @Override
    public PeriodCloseCheckResultDto run(LocalDate periodEndDate) {
        List<VendorBill> drafts =
                vendorBillRepository.findByStatusAndBillDateLessThanEqual(VendorBillStatus.DRAFT, periodEndDate);

        List<PeriodCloseCheckItemDto> items = drafts.stream()
                .map(b -> PeriodCloseCheckItemDto.builder()
                        .id(b.getId())
                        .reference(b.getBillNumber())
                        .date(b.getBillDate())
                        .amount(b.getGrandTotal())
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