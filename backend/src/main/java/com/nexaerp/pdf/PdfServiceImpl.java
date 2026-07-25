package com.nexaerp.pdf;

import com.nexaerp.pdf.invoice.InvoicePdfGenerator;
import com.nexaerp.pdf.payment.PaymentReceiptGenerator;
import com.nexaerp.pdf.reports.LedgerPdfGenerator;
import com.nexaerp.pdf.reports.PartyStatementPdfGenerator;
import com.nexaerp.pdf.reports.TrialBalancePdfGenerator;
import com.nexaerp.pdf.vendorbill.VendorBillPdfGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PdfServiceImpl implements PdfService {

    private final InvoicePdfGenerator invoicePdfGenerator;
    private final VendorBillPdfGenerator vendorBillPdfGenerator;
    private final LedgerPdfGenerator ledgerPdfGenerator;
    private final TrialBalancePdfGenerator trialBalancePdfGenerator;
    private final PartyStatementPdfGenerator partyStatementPdfGenerator;
    private final PaymentReceiptGenerator paymentReceiptGenerator;

    @Override
    public byte[] generateInvoicePdf(Long invoiceId) {
        return invoicePdfGenerator.generate(invoiceId);
    }

    @Override
    public byte[] generateVendorBillPdf(Long billId) {
        return vendorBillPdfGenerator.generate(billId);
    }

    @Override
    public byte[] generateLedgerPdf(Long accountId, String fromDate, String toDate) {
        return ledgerPdfGenerator.generate(accountId, fromDate, toDate);
    }

    @Override
    public byte[] generateTrialBalancePdf(String asOfDate) {
        return trialBalancePdfGenerator.generate(asOfDate);
    }

    @Override
    public byte[] generatePartyStatementPdf(Long partyId, String fromDate, String toDate) {
        return partyStatementPdfGenerator.generate(partyId, fromDate, toDate);
    }

    @Override
    public byte[] generatePaymentReceiptPdf(Long paymentId) {
        return paymentReceiptGenerator.generate(paymentId);
    }
}