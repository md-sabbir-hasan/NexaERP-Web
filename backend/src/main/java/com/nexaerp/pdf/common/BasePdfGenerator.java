package com.nexaerp.pdf.common;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;

import java.io.ByteArrayOutputStream;

public abstract class BasePdfGenerator {

    protected byte[] buildPdf(
            String title,
            String documentNumber,
            PageSize pageSize,
            PdfContentWriter writer
    ) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        PdfWriter pdfWriter = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(pdfWriter);

        Document doc = new Document(pdf, pageSize);

        doc.setMargins(
                PdfConstants.PAGE_MARGIN,
                PdfConstants.PAGE_MARGIN,
                PdfConstants.PAGE_MARGIN,
                PdfConstants.PAGE_MARGIN
        );

        PdfHeader.add(doc, title, documentNumber);

        writer.write(doc);

        PdfFooter.add(doc);

        doc.close();

        return baos.toByteArray();
    }

    protected byte[] buildA4(
            String title,
            String documentNumber,
            PdfContentWriter writer
    ) {
        return buildPdf(title, documentNumber, PageSize.A4, writer);
    }

    protected byte[] buildA4Landscape(
            String title,
            String documentNumber,
            PdfContentWriter writer
    ) {
        return buildPdf(title, documentNumber, PageSize.A4.rotate(), writer);
    }

    @FunctionalInterface
    protected interface PdfContentWriter {
        void write(Document doc);
    }
}