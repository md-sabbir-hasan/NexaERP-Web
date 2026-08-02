package com.nexaerp.overdue;

import com.nexaerp.invoice.Invoice;
import com.nexaerp.invoice.InvoiceStatus;
import com.nexaerp.vendorbill.VendorBill;
import com.nexaerp.vendorbill.VendorBillStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Component
public class OverdueEligibility {

    private static final Set<InvoiceStatus> INVOICE_STATUSES =
            Set.of(InvoiceStatus.POSTED, InvoiceStatus.PARTIAL);
    private static final Set<VendorBillStatus> VENDOR_BILL_STATUSES =
            Set.of(VendorBillStatus.POSTED, VendorBillStatus.PARTIAL);

    public boolean isEligible(Invoice invoice, LocalDate businessDate) {
        return invoice != null
                && businessDate != null
                && invoice.getDueDate() != null
                && invoice.getDueDate().isBefore(businessDate)
                && invoice.getDueAmount() != null
                && invoice.getDueAmount().compareTo(BigDecimal.ZERO) > 0
                && INVOICE_STATUSES.contains(invoice.getStatus());
    }

    public boolean isEligible(VendorBill bill, LocalDate businessDate) {
        return bill != null
                && businessDate != null
                && bill.getDueDate() != null
                && bill.getDueDate().isBefore(businessDate)
                && bill.getDueAmount() != null
                && bill.getDueAmount().compareTo(BigDecimal.ZERO) > 0
                && VENDOR_BILL_STATUSES.contains(bill.getStatus());
    }
}
