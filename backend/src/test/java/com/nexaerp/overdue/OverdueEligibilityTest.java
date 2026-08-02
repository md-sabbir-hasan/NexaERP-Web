package com.nexaerp.overdue;

import com.nexaerp.invoice.Invoice;
import com.nexaerp.invoice.InvoiceStatus;
import com.nexaerp.vendorbill.VendorBill;
import com.nexaerp.vendorbill.VendorBillStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverdueEligibilityTest {

    private final OverdueEligibility eligibility = new OverdueEligibility();
    private final LocalDate today = LocalDate.of(2026, 8, 10);

    @Test
    void invoiceRequiresPastDuePositiveAmountAndPostedOrPartialStatus() {
        for (InvoiceStatus status : new InvoiceStatus[]{InvoiceStatus.POSTED, InvoiceStatus.PARTIAL}) {
            assertTrue(eligibility.isEligible(invoice(today.minusDays(1), BigDecimal.ONE, status), today));
        }
        for (InvoiceStatus status : new InvoiceStatus[]{
                InvoiceStatus.DRAFT, InvoiceStatus.PAID, InvoiceStatus.CANCELLED}) {
            assertFalse(eligibility.isEligible(invoice(today.minusDays(1), BigDecimal.ONE, status), today));
        }
        assertFalse(eligibility.isEligible(invoice(today, BigDecimal.ONE, InvoiceStatus.POSTED), today));
        assertFalse(eligibility.isEligible(invoice(today.plusDays(1), BigDecimal.ONE, InvoiceStatus.POSTED), today));
        assertFalse(eligibility.isEligible(invoice(null, BigDecimal.ONE, InvoiceStatus.POSTED), today));
        assertFalse(eligibility.isEligible(invoice(today.minusDays(1), null, InvoiceStatus.POSTED), today));
        assertFalse(eligibility.isEligible(invoice(today.minusDays(1), BigDecimal.ZERO, InvoiceStatus.POSTED), today));
        assertFalse(eligibility.isEligible(invoice(
                today.minusDays(1), BigDecimal.ONE.negate(), InvoiceStatus.POSTED), today));
    }

    @Test
    void vendorBillExcludesApprovedAndUsesTheSameOutstandingRule() {
        for (VendorBillStatus status : new VendorBillStatus[]{
                VendorBillStatus.POSTED, VendorBillStatus.PARTIAL}) {
            assertTrue(eligibility.isEligible(bill(today.minusDays(1), BigDecimal.ONE, status), today));
        }
        for (VendorBillStatus status : new VendorBillStatus[]{
                VendorBillStatus.DRAFT, VendorBillStatus.APPROVED,
                VendorBillStatus.PAID, VendorBillStatus.CANCELLED}) {
            assertFalse(eligibility.isEligible(bill(today.minusDays(1), BigDecimal.ONE, status), today));
        }
        assertFalse(eligibility.isEligible(bill(today, BigDecimal.ONE, VendorBillStatus.POSTED), today));
        assertFalse(eligibility.isEligible(bill(today.plusDays(1), BigDecimal.ONE, VendorBillStatus.POSTED), today));
        assertFalse(eligibility.isEligible(bill(null, BigDecimal.ONE, VendorBillStatus.POSTED), today));
        assertFalse(eligibility.isEligible(bill(today.minusDays(1), null, VendorBillStatus.POSTED), today));
        assertFalse(eligibility.isEligible(bill(today.minusDays(1), BigDecimal.ZERO, VendorBillStatus.POSTED), today));
        assertFalse(eligibility.isEligible(bill(
                today.minusDays(1), BigDecimal.ONE.negate(), VendorBillStatus.POSTED), today));
    }

    private Invoice invoice(LocalDate dueDate, BigDecimal dueAmount, InvoiceStatus status) {
        return Invoice.builder().dueDate(dueDate).dueAmount(dueAmount).status(status).build();
    }

    private VendorBill bill(LocalDate dueDate, BigDecimal dueAmount, VendorBillStatus status) {
        return VendorBill.builder().dueDate(dueDate).dueAmount(dueAmount).status(status).build();
    }
}
