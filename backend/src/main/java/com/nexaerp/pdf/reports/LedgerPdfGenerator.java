package com.nexaerp.pdf.reports;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.UnitValue;
import com.nexaerp.pdf.common.*;
import com.nexaerp.report.ReportService;
import com.nexaerp.report.dto.LedgerEntryDto;
import com.nexaerp.report.dto.LedgerResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class LedgerPdfGenerator {

    private final ReportService reportService;

    public byte[] generate(Long accountId, String fromDate, String toDate) {

        LocalDate from = LocalDate.parse(fromDate);
        LocalDate to = LocalDate.parse(toDate);

        LedgerResponseDto ledger =
                reportService.getLedger(accountId, from, to);

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

        PdfHeader.add(doc, "LEDGER REPORT", ledger.getAccountCode() + " - " + ledger.getAccountName());

        addReportInfo(doc, ledger);

        addEntriesTable(doc, ledger);

        addSummary(doc, ledger);

        PdfFooter.add(doc);

        doc.close();

        return baos.toByteArray();
    }

    private void addReportInfo(Document doc, LedgerResponseDto ledger) {

        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1}))
                .useAllAvailableWidth()
                .setMarginBottom(16);

        table.addCell(PdfTableHelper.textCell("Account Code: " + PdfFormatUtil.text(ledger.getAccountCode())));
        table.addCell(PdfTableHelper.textCell("Account Name: " + PdfFormatUtil.text(ledger.getAccountName())));
        table.addCell(PdfTableHelper.textCell("Account Type: " + ledger.getAccountType()));
        table.addCell(PdfTableHelper.textCell(
                "Period: " + PdfFormatUtil.date(ledger.getFromDate()) + " to " + PdfFormatUtil.date(ledger.getToDate())
        ));

        doc.add(table);
    }

    private void addEntriesTable(Document doc, LedgerResponseDto ledger) {

        Table table = new Table(UnitValue.createPercentArray(
                new float[]{1.2f, 1.4f, 1.4f, 3f, 1.2f, 1.2f, 1.4f}
        )).useAllAvailableWidth();

        String[] headers = {
                "Date",
                "Journal No",
                "Reference",
                "Description",
                "Debit",
                "Credit",
                "Running Balance"
        };

        for (String h : headers) {
            table.addHeaderCell(PdfTableHelper.headerCell(h));
        }

        table.addCell(PdfTableHelper.textCell(PdfFormatUtil.date(ledger.getFromDate())));
        table.addCell(PdfTableHelper.textCell("-"));
        table.addCell(PdfTableHelper.textCell("-"));
        table.addCell(PdfTableHelper.textCell("Opening Balance"));
        table.addCell(PdfTableHelper.amountCell("-"));
        table.addCell(PdfTableHelper.amountCell("-"));
        table.addCell(PdfTableHelper.amountCell(PdfFormatUtil.money(ledger.getOpeningBalance())));

        for (LedgerEntryDto entry : ledger.getEntries()) {
            table.addCell(PdfTableHelper.textCell(PdfFormatUtil.date(entry.getDate())));
            table.addCell(PdfTableHelper.textCell(PdfFormatUtil.text(entry.getJournalEntryNumber())));
            table.addCell(PdfTableHelper.textCell(PdfFormatUtil.text(entry.getReferenceNumber())));
            table.addCell(PdfTableHelper.textCell(PdfFormatUtil.text(entry.getDescription())));
            table.addCell(PdfTableHelper.amountCell(PdfFormatUtil.money(entry.getDebit())));
            table.addCell(PdfTableHelper.amountCell(PdfFormatUtil.money(entry.getCredit())));
            table.addCell(PdfTableHelper.amountCell(PdfFormatUtil.money(entry.getRunningBalance())));
        }

        doc.add(table);
    }

    private void addSummary(Document doc, LedgerResponseDto ledger) {
        PdfSummaryHelper.addSummary(doc, new String[][]{
                {"Opening Balance:", PdfFormatUtil.money(ledger.getOpeningBalance())},
                {"Total Debit:", PdfFormatUtil.money(ledger.getTotalDebit())},
                {"Total Credit:", PdfFormatUtil.money(ledger.getTotalCredit())},
                {"Closing Balance:", PdfFormatUtil.money(ledger.getClosingBalance()), "bold"}
        });
    }
}