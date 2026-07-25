package com.nexaerp.pdf.common;

import com.itextpdf.kernel.colors.Color;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;

public final class PdfTableHelper {

    private PdfTableHelper() {}

    public static Cell headerCell(String text) {
        return new Cell()
                .setBackgroundColor(PdfConstants.PRIMARY)
                .setBorder(Border.NO_BORDER)
                .setPadding(6)
                .add(new Paragraph(text)
                        .setBold()
                        .setFontSize(8)
                        .setFontColor(PdfConstants.WHITE));
    }

    public static Cell textCell(String text) {
        return bodyCell(text, TextAlignment.LEFT);
    }

    public static Cell centerCell(String text) {
        return bodyCell(text, TextAlignment.CENTER);
    }

    public static Cell amountCell(String text) {
        return bodyCell(text, TextAlignment.RIGHT);
    }

    private static Cell bodyCell(String text, TextAlignment alignment) {
        return new Cell()
                .setBorderLeft(Border.NO_BORDER)
                .setBorderRight(Border.NO_BORDER)
                .setBorderTop(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder((Color) PdfConstants.BORDER, 0.5f))
                .setPadding(6)
                .add(new Paragraph(text)
                        .setFontSize(8.5f)
                        .setTextAlignment(alignment));
    }
}