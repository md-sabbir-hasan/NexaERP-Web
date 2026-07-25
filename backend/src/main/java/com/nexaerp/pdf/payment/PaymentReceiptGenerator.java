package com.nexaerp.pdf.payment;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.payment.Payment;
import com.nexaerp.payment.PaymentAllocation;
import com.nexaerp.payment.PaymentAllocationRepository;
import com.nexaerp.payment.PaymentRepository;
import com.nexaerp.pdf.common.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentReceiptGenerator {

    private final PaymentRepository paymentRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;

    public byte[] generate(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        List<PaymentAllocation> allocations =
                paymentAllocationRepository.findByPaymentId(paymentId);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf, PageSize.A4);

        doc.setMargins(
                PdfConstants.PAGE_MARGIN,
                PdfConstants.PAGE_MARGIN,
                PdfConstants.PAGE_MARGIN,
                PdfConstants.PAGE_MARGIN
        );

        PdfHeader.add(doc, "PAYMENT RECEIPT", payment.getPaymentNumber());

        addPaymentInfo(doc, payment);

        if (!allocations.isEmpty()) {
            PaymentReceiptHelper.addAllocations(doc, allocations);
        }

        PaymentReceiptHelper.addSummary(doc, payment);

        addNotes(doc, payment.getNotes());

        addSignatureArea(doc);

        PdfFooter.add(doc);

        doc.close();

        return baos.toByteArray();
    }

    private void addPaymentInfo(Document doc, Payment payment) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth()
                .setMarginBottom(18);

        Cell left = new Cell()
                .setBorder(null)
                .add(new Paragraph("PARTY")
                        .setBold()
                        .setFontSize(8)
                        .setFontColor(PdfConstants.MUTED))
                .add(new Paragraph(PdfFormatUtil.text(payment.getParty().getName()))
                        .setBold()
                        .setFontSize(11))
                .add(new Paragraph(PdfFormatUtil.text(payment.getParty().getPhone()))
                        .setFontSize(9))
                .add(new Paragraph(PdfFormatUtil.text(payment.getParty().getEmail()))
                        .setFontSize(9));

        Cell right = new Cell()
                .setBorder(null)
                .setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph("Date: " + PdfFormatUtil.date(payment.getPaymentDate()))
                        .setFontSize(9))
                .add(new Paragraph("Type: " + payment.getPaymentType().name())
                        .setFontSize(9))
                .add(new Paragraph("Method: " + payment.getPaymentMethod().name())
                        .setFontSize(9))
                .add(new Paragraph("Account: " + payment.getAccount().getName())
                        .setFontSize(9))
                .add(new Paragraph("Transaction Ref: " + PdfFormatUtil.text(payment.getTransactionRef()))
                        .setFontSize(9))
                .add(new Paragraph("Status: " + payment.getStatus().name())
                        .setFontSize(9));

        table.addCell(left);
        table.addCell(right);

        doc.add(table);
    }

    private void addNotes(Document doc, String notes) {
        if (notes == null || notes.isBlank()) {
            return;
        }

        doc.add(new Paragraph("Notes")
                .setBold()
                .setFontSize(9)
                .setFontColor(PdfConstants.MUTED)
                .setMarginTop(8));

        doc.add(new Paragraph(notes)
                .setFontSize(9)
                .setMarginBottom(12));
    }

    private void addSignatureArea(Document doc) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth()
                .setMarginTop(36);

        Cell preparedBy = new Cell()
                .setBorder(null)
                .setTextAlignment(TextAlignment.CENTER)
                .add(new Paragraph("________________________")
                        .setFontSize(9))
                .add(new Paragraph("Prepared By")
                        .setFontSize(8)
                        .setFontColor(PdfConstants.MUTED));

        Cell receivedBy = new Cell()
                .setBorder(null)
                .setTextAlignment(TextAlignment.CENTER)
                .add(new Paragraph("________________________")
                        .setFontSize(9))
                .add(new Paragraph("Received / Approved By")
                        .setFontSize(8)
                        .setFontColor(PdfConstants.MUTED));

        table.addCell(preparedBy);
        table.addCell(receivedBy);

        doc.add(table);
    }
}