package com.nexaerp.pdf.vendorbill;

import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import com.nexaerp.pdf.common.PdfFormatUtil;
import com.nexaerp.pdf.common.PdfSummaryHelper;
import com.nexaerp.pdf.common.PdfTableHelper;
import com.nexaerp.vendorbill.VendorBill;
import com.nexaerp.vendorbill.VendorBillItem;

import java.util.List;

public final class VendorBillPdfHelper {

    private VendorBillPdfHelper() {}

    public static void addItems(Document doc, List<VendorBillItem> items) {
        Table table = new Table(UnitValue.createPercentArray(
                new float[]{2, 2.4f, 1, 1.3f, 1.1f, 1.1f, 1.1f, 1.4f}
        )).useAllAvailableWidth();

        String[] headers = {
                "Account", "Description", "Qty", "Unit Price",
                "Discount", "VAT", "TDS", "Total"
        };

        for (String h : headers) {
            table.addHeaderCell(PdfTableHelper.headerCell(h));
        }

        for (VendorBillItem item : items) {
            table.addCell(PdfTableHelper.textCell(
                    item.getExpenseAccount().getCode() + " - " + item.getExpenseAccount().getName()
            ));
            table.addCell(PdfTableHelper.textCell(PdfFormatUtil.text(item.getDescription())));
            table.addCell(PdfTableHelper.centerCell(item.getQuantity().toPlainString()));
            table.addCell(PdfTableHelper.amountCell(PdfFormatUtil.money(item.getUnitPrice())));
            table.addCell(PdfTableHelper.amountCell(PdfFormatUtil.money(item.getDiscountAmount())));
            table.addCell(PdfTableHelper.amountCell(PdfFormatUtil.money(item.getVatAmount())));
            table.addCell(PdfTableHelper.amountCell(PdfFormatUtil.money(item.getTdsAmount())));
            table.addCell(PdfTableHelper.amountCell(PdfFormatUtil.money(item.getLineTotal())));
        }

        doc.add(table);
    }

    public static void addSummary(Document doc, VendorBill bill) {
        PdfSummaryHelper.addSummary(doc, new String[][]{
                {"Subtotal:", PdfFormatUtil.money(bill.getSubTotal())},
                {"Discount:", PdfFormatUtil.money(bill.getDiscountAmount())},
                {"VAT:", PdfFormatUtil.money(bill.getVatAmount())},
                {"TDS:", PdfFormatUtil.money(bill.getTdsAmount())},
                {"Grand Total:", PdfFormatUtil.money(bill.getGrandTotal()), "bold"},
                {"Net Payable:", PdfFormatUtil.money(bill.getNetPayable()), "bold"},
                {"Paid:", PdfFormatUtil.money(bill.getPaidAmount())},
                {"Due:", PdfFormatUtil.money(bill.getDueAmount()), "bold"}
        });
    }
}