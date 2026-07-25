package com.nexaerp.pdf;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PdfController {

    private final PdfService pdfService;

    @GetMapping("/api/invoices/{id}/pdf")
    @PreAuthorize("hasAuthority('VIEW_INVOICE')")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable Long id) {
        byte[] pdfBytes = pdfService.generateInvoicePdf(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"invoice-" + id + ".pdf\""
                )
                .body(pdfBytes);
    }

    @GetMapping("/api/vendor-bills/{id}/pdf")
    @PreAuthorize("hasAuthority('VIEW_VENDOR_BILL')")
    public ResponseEntity<byte[]> downloadVendorBillPdf(@PathVariable Long id) {
        byte[] pdfBytes = pdfService.generateVendorBillPdf(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"vendor-bill-" + id + ".pdf\""
                )
                .body(pdfBytes);
    }


    @GetMapping("/api/reports/ledger/pdf")
    @PreAuthorize("hasAuthority('VIEW_LEDGER')")
    public ResponseEntity<byte[]> downloadLedgerPdf(
            @RequestParam Long accountId,
            @RequestParam String fromDate,
            @RequestParam String toDate) {

        byte[] pdfBytes = pdfService.generateLedgerPdf(accountId, fromDate, toDate);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"ledger-report.pdf\"")
                .body(pdfBytes);
    }

    @GetMapping("/api/reports/trial-balance/pdf")
    @PreAuthorize("hasAuthority('VIEW_TRIAL_BALANCE')")
    public ResponseEntity<byte[]> downloadTrialBalancePdf(
            @RequestParam String asOfDate) {

        byte[] pdfBytes = pdfService.generateTrialBalancePdf(asOfDate);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"trial-balance.pdf\"")
                .body(pdfBytes);
    }

    @GetMapping("/api/reports/party-statement/pdf")
    @PreAuthorize("hasAuthority('VIEW_REPORT')")
    public ResponseEntity<byte[]> downloadPartyStatementPdf(
            @RequestParam Long partyId,
            @RequestParam String fromDate,
            @RequestParam String toDate) {

        byte[] pdfBytes = pdfService.generatePartyStatementPdf(partyId, fromDate, toDate);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"party-statement.pdf\"")
                .body(pdfBytes);
    }

    @GetMapping("/api/payments/{id}/receipt")
    @PreAuthorize("hasAuthority('VIEW_PAYMENT')")
    public ResponseEntity<byte[]> downloadPaymentReceipt(
            @PathVariable Long id) {

        byte[] pdf = pdfService.generatePaymentReceiptPdf(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"payment-receipt.pdf\"")
                .body(pdf);
    }
}