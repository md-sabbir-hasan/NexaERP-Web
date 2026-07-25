package com.nexaerp.pdf.reports;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import com.nexaerp.pdf.common.*;
import com.nexaerp.report.ReportService;
import com.nexaerp.report.dto.PartyStatementEntryDto;
import com.nexaerp.report.dto.PartyStatementResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class PartyStatementPdfGenerator {

    private final ReportService reportService;

    public byte[] generate(Long partyId, String fromDate, String toDate) {
        LocalDate from = LocalDate.parse(fromDate);
        LocalDate to = LocalDate.parse(toDate);

        PartyStatementResponseDto statement =
                reportService.getPartyStatement(partyId, from, to);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf, PageSize.A4.rotate());

        doc.setMargins(
                PdfConstants.PAGE_MARGIN,
                PdfConstants.PAGE_MARGIN,
                PdfConstants.PAGE_MARGIN,
                PdfConstants.PAGE_MARGIN
        );

        PdfHeader.add(doc, "PARTY STATEMENT", statement.getPartyName());

        addInfo(doc, statement);
        addTable(doc, statement);
        addSummary(doc, statement);

        PdfFooter.add(doc);
        doc.close();

        return baos.toByteArray();
    }

    private void addInfo(Document doc, PartyStatementResponseDto statement) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1}))
                .useAllAvailableWidth()
                .setMarginBottom(16);

        table.addCell(PdfTableHelper.textCell("Party: " + PdfFormatUtil.text(statement.getPartyName())));
        table.addCell(PdfTableHelper.textCell("Type: " + PdfFormatUtil.text(statement.getPartyType())));
        table.addCell(PdfTableHelper.textCell("From: " + PdfFormatUtil.date(statement.getFromDate())));
        table.addCell(PdfTableHelper.textCell("To: " + PdfFormatUtil.date(statement.getToDate())));

        doc.add(table);
    }

    private void addTable(Document doc, PartyStatementResponseDto statement) {
        Table table = new Table(UnitValue.createPercentArray(
                new float[]{1.1f, 1.2f, 1.4f, 3f, 1.2f, 1.2f, 1.4f}
        )).useAllAvailableWidth();

        String[] headers = {
                "Date",
                "Type",
                "Reference",
                "Description",
                "Debit",
                "Credit",
                "Balance"
        };

        for (String h : headers) {
            table.addHeaderCell(PdfTableHelper.headerCell(h));
        }

        table.addCell(PdfTableHelper.textCell(PdfFormatUtil.date(statement.getFromDate())));
        table.addCell(PdfTableHelper.textCell("-"));
        table.addCell(PdfTableHelper.textCell("-"));
        table.addCell(PdfTableHelper.textCell("Opening Balance"));
        table.addCell(PdfTableHelper.amountCell("-"));
        table.addCell(PdfTableHelper.amountCell("-"));
        table.addCell(PdfTableHelper.amountCell(PdfFormatUtil.money(statement.getOpeningBalance())));

        for (PartyStatementEntryDto entry : statement.getEntries()) {
            table.addCell(PdfTableHelper.textCell(PdfFormatUtil.date(entry.getDate())));
            table.addCell(PdfTableHelper.textCell(entry.getType().name()));
            table.addCell(PdfTableHelper.textCell(PdfFormatUtil.text(entry.getReferenceNumber())));
            table.addCell(PdfTableHelper.textCell(PdfFormatUtil.text(entry.getDescription())));
            table.addCell(PdfTableHelper.amountCell(PdfFormatUtil.money(entry.getDebit())));
            table.addCell(PdfTableHelper.amountCell(PdfFormatUtil.money(entry.getCredit())));
            table.addCell(PdfTableHelper.amountCell(PdfFormatUtil.money(entry.getRunningBalance())));
        }

        doc.add(table);
    }

    private void addSummary(Document doc, PartyStatementResponseDto statement) {
        PdfSummaryHelper.addSummary(doc, new String[][]{
                {"Opening Balance:", PdfFormatUtil.money(statement.getOpeningBalance())},
                {"Closing Balance:", PdfFormatUtil.money(statement.getClosingBalance()), "bold"}
        });
    }
}