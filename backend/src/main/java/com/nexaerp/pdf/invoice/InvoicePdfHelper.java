package com.nexaerp.pdf.invoice;

import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import com.nexaerp.invoice.Invoice;
import com.nexaerp.invoice.InvoiceItem;
import com.nexaerp.pdf.common.PdfFormatUtil;
import com.nexaerp.pdf.common.PdfSummaryHelper;
import com.nexaerp.pdf.common.PdfTableHelper;

import java.util.List;

public final class InvoicePdfHelper {

    private InvoicePdfHelper() {}

    public static void addItems(Document doc, List<InvoiceItem> items) {
        Table table = new Table(UnitValue.createPercentArray(
                new float[]{3, 1, 1.4f, 1.2f, 1.2f, 1.5f}
        )).useAllAvailableWidth();

        String[] headers = {"Description", "Qty", "Unit Price", "Discount", "VAT", "Total"};

        for (String h : headers) {
            table.addHeaderCell(PdfTableHelper.headerCell(h));
        }

        for (InvoiceItem item : items) {
            table.addCell(PdfTableHelper.textCell(PdfFormatUtil.text(item.getDescription())));
            table.addCell(PdfTableHelper.centerCell(item.getQuantity().toPlainString()));
            table.addCell(PdfTableHelper.amountCell(PdfFormatUtil.money(item.getUnitPrice())));
            table.addCell(PdfTableHelper.amountCell(PdfFormatUtil.money(item.getDiscountAmount())));
            table.addCell(PdfTableHelper.amountCell(PdfFormatUtil.money(item.getVatAmount())));
            table.addCell(PdfTableHelper.amountCell(PdfFormatUtil.money(item.getLineTotal())));
        }

        doc.add(table);
    }

    public static void addSummary(Document doc, Invoice invoice) {
        PdfSummaryHelper.addSummary(doc, new String[][]{
                {"Subtotal:", PdfFormatUtil.money(invoice.getSubTotal())},
                {"Discount:", PdfFormatUtil.money(invoice.getDiscountAmount())},
                {"VAT:", PdfFormatUtil.money(invoice.getVatAmount())},
                {"Grand Total:", PdfFormatUtil.money(invoice.getGrandTotal()), "bold"},
                {"Paid:", PdfFormatUtil.money(invoice.getPaidAmount())},
                {"Due:", PdfFormatUtil.money(invoice.getDueAmount()), "bold"}
        });
    }
}