package com.nexaerp.pdf.vendorbill;

import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.pdf.common.BasePdfGenerator;
import com.nexaerp.pdf.common.PdfConstants;
import com.nexaerp.pdf.common.PdfFormatUtil;
import com.nexaerp.vendorbill.VendorBill;
import com.nexaerp.vendorbill.VendorBillItem;
import com.nexaerp.vendorbill.VendorBillItemRepository;
import com.nexaerp.vendorbill.VendorBillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class VendorBillPdfGenerator extends BasePdfGenerator {

    private final VendorBillRepository vendorBillRepository;
    private final VendorBillItemRepository vendorBillItemRepository;

    public byte[] generate(Long billId) {
        VendorBill bill = vendorBillRepository.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor bill not found"));

        List<VendorBillItem> items =
                vendorBillItemRepository.findByVendorBillId(billId);

        return buildA4(
                "VENDOR BILL",
                bill.getBillNumber(),
                doc -> {
                    addBillInfo(doc, bill);
                    VendorBillPdfHelper.addItems(doc, items);
                    VendorBillPdfHelper.addSummary(doc, bill);
                    addNotes(doc, bill.getNotes());
                }
        );
    }

    private void addBillInfo(Document doc, VendorBill bill) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth()
                .setMarginBottom(18);

        Cell left = new Cell()
                .setBorder(null)
                .add(new Paragraph("VENDOR")
                        .setBold()
                        .setFontSize(8)
                        .setFontColor(PdfConstants.MUTED))
                .add(new Paragraph(PdfFormatUtil.text(bill.getParty().getName()))
                        .setBold()
                        .setFontSize(11))
                .add(new Paragraph(PdfFormatUtil.text(bill.getParty().getPhone()))
                        .setFontSize(9))
                .add(new Paragraph(PdfFormatUtil.text(bill.getParty().getEmail()))
                        .setFontSize(9));

        Cell right = new Cell()
                .setBorder(null)
                .setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph("Bill Date: " + PdfFormatUtil.date(bill.getBillDate()))
                        .setFontSize(9))
                .add(new Paragraph("Posting Date: " + PdfFormatUtil.date(bill.getPostingDate()))
                        .setFontSize(9))
                .add(new Paragraph("Due Date: " + PdfFormatUtil.date(bill.getDueDate()))
                        .setFontSize(9))
                .add(new Paragraph("Vendor Ref: " + PdfFormatUtil.text(bill.getVendorBillRef()))
                        .setFontSize(9))
                .add(new Paragraph("Status: " + bill.getStatus().name())
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