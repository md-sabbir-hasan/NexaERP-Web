package com.nexaerp.pdf.invoice;

import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.invoice.Invoice;
import com.nexaerp.invoice.InvoiceItem;
import com.nexaerp.invoice.InvoiceItemRepository;
import com.nexaerp.invoice.InvoiceRepository;
import com.nexaerp.pdf.common.BasePdfGenerator;
import com.nexaerp.pdf.common.PdfConstants;
import com.nexaerp.pdf.common.PdfFormatUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class InvoicePdfGenerator extends BasePdfGenerator {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;

    @Transactional
    public byte[] generate(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        List<InvoiceItem> items =
                invoiceItemRepository.findByInvoiceId(invoiceId);

        byte[] pdf = buildA4(
                "TAX INVOICE",
                invoice.getInvoiceNumber(),
                doc -> {
                    addInvoiceInfo(doc, invoice);
                    InvoicePdfHelper.addItems(doc, items);
                    InvoicePdfHelper.addSummary(doc, invoice);
                    addNotes(doc, invoice.getNotes());
                }
        );

        invoice.setPdfGenerated(true);
        invoice.setPrintCount(
                invoice.getPrintCount() == null ? 1 : invoice.getPrintCount() + 1
        );
        invoiceRepository.save(invoice);

        return pdf;
    }

    private void addInvoiceInfo(Document doc, Invoice invoice) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth()
                .setMarginBottom(18);

        Cell left = new Cell()
                .setBorder(null)
                .add(new Paragraph("BILL TO")
                        .setBold()
                        .setFontSize(8)
                        .setFontColor(PdfConstants.MUTED))
                .add(new Paragraph(PdfFormatUtil.text(invoice.getParty().getName()))
                        .setBold()
                        .setFontSize(11))
                .add(new Paragraph(PdfFormatUtil.text(invoice.getParty().getPhone()))
                        .setFontSize(9))
                .add(new Paragraph(PdfFormatUtil.text(invoice.getParty().getEmail()))
                        .setFontSize(9));

        Cell right = new Cell()
                .setBorder(null)
                .setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph("Invoice Date: " + PdfFormatUtil.date(invoice.getInvoiceDate()))
                        .setFontSize(9))
                .add(new Paragraph("Due Date: " + PdfFormatUtil.date(invoice.getDueDate()))
                        .setFontSize(9))
                .add(new Paragraph("Reference: " + PdfFormatUtil.text(invoice.getReference()))
                        .setFontSize(9))
                .add(new Paragraph("Status: " + invoice.getStatus().name())
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
}