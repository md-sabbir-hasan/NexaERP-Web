package com.nexaerp.pdf.common;

import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

public final class PdfHeader {

    private PdfHeader() {}

    public static void add(Document doc, String title, String number) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth();

        Cell left = new Cell()
                .setBorder(null)
                .add(new Paragraph("NexaERP")
                        .setBold()
                        .setFontSize(22)
                        .setFontColor(PdfConstants.PRIMARY))
                .add(new Paragraph("Financial Management System")
                        .setFontSize(9)
                        .setFontColor(PdfConstants.MUTED));

        Cell right = new Cell()
                .setBorder(null)
                .setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph(title)
                        .setBold()
                        .setFontSize(17)
                        .setFontColor(PdfConstants.DARK))
                .add(new Paragraph(number)
                        .setBold()
                        .setFontSize(11)
                        .setFontColor(PdfConstants.PRIMARY));

        table.addCell(left);
        table.addCell(right);

        doc.add(table);
        doc.add(new LineSeparator(new SolidLine(1.2f))
                .setMarginTop(10)
                .setMarginBottom(18));
    }
}