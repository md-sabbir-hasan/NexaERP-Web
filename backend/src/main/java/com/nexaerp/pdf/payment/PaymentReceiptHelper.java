package com.nexaerp.pdf.payment;

import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import com.nexaerp.payment.Payment;
import com.nexaerp.payment.PaymentAllocation;
import com.nexaerp.pdf.common.PdfFormatUtil;
import com.nexaerp.pdf.common.PdfSummaryHelper;
import com.nexaerp.pdf.common.PdfTableHelper;

import java.util.List;

public final class PaymentReceiptHelper {

    private PaymentReceiptHelper() {}

    public static void addAllocations(Document doc, List<PaymentAllocation> allocations) {
        Table table = new Table(UnitValue.createPercentArray(
                new float[]{1.5f, 1.5f, 3f, 1.5f}
        )).useAllAvailableWidth();

        table.addHeaderCell(PdfTableHelper.headerCell("Type"));
        table.addHeaderCell(PdfTableHelper.headerCell("Reference ID"));
        table.addHeaderCell(PdfTableHelper.headerCell("Description"));
        table.addHeaderCell(PdfTableHelper.headerCell("Allocated"));

        for (PaymentAllocation allocation : allocations) {
            table.addCell(PdfTableHelper.textCell(allocation.getReferenceType().name()));
            table.addCell(PdfTableHelper.textCell(String.valueOf(allocation.getReferenceId())));
            table.addCell(PdfTableHelper.textCell("Allocation against " + allocation.getReferenceType().name()));
            table.addCell(PdfTableHelper.amountCell(PdfFormatUtil.money(allocation.getAllocatedAmount())));
        }

        doc.add(table);
    }

    public static void addSummary(Document doc, Payment payment) {
        PdfSummaryHelper.addSummary(doc, new String[][]{
                {"Amount:", PdfFormatUtil.money(payment.getAmount()), "bold"},
                {"Allocated:", PdfFormatUtil.money(payment.getAllocatedAmount())},
                {"Unallocated:", PdfFormatUtil.money(payment.getUnallocatedAmount()), "bold"}
        });
    }
}