package com.nexaerp.pdf;

public interface PdfService {

    byte[] generateInvoicePdf(Long invoiceId);

    byte[] generateVendorBillPdf(Long billId);

    byte[] generateLedgerPdf(Long accountId, String fromDate, String toDate);

    byte[] generateTrialBalancePdf(String asOfDate);
    byte[] generatePartyStatementPdf(Long partyId, String fromDate, String toDate);
    byte[] generatePaymentReceiptPdf(Long paymentId);
}