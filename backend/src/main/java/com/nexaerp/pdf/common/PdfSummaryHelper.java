package com.nexaerp.pdf.common;

import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

public final class PdfSummaryHelper {

    private PdfSummaryHelper() {}

    public static void addSummary(Document doc, String[][] rows) {
        Table outer = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth()
                .setMarginTop(10)
                .setMarginBottom(16);

        outer.addCell(new Cell().setBorder(null));

        Cell summary = new Cell()
                .setBorder(new SolidBorder(PdfConstants.BORDER, 1))
                .setBackgroundColor(PdfConstants.LIGHT)
                .setPadding(12);

        for (String[] row : rows) {
            boolean bold = row.length > 2 && "bold".equals(row[2]);

            Paragraph p = new Paragraph()
                    .add(new Text(row[0] + "  ")
                            .setFontSize(9)
                            .setFontColor(PdfConstants.MUTED))
                    .add(new Text(row[1])
                            .setFontSize(9));

            if (bold) {
                p.setBold().setFontSize(10);
            }

            p.setTextAlignment(TextAlignment.RIGHT);
            summary.add(p);

            if (bold) {
                summary.add(new LineSeparator(new SolidLine(0.4f))
                        .setMarginTop(3)
                        .setMarginBottom(3));
            }
        }

        outer.addCell(summary);
        doc.add(outer);
    }
}