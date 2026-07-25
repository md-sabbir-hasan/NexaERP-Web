package com.nexaerp.pdf.reports;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import com.nexaerp.pdf.common.PdfConstants;
import com.nexaerp.pdf.common.PdfFooter;
import com.nexaerp.pdf.common.PdfFormatUtil;
import com.nexaerp.pdf.common.PdfHeader;
import com.nexaerp.pdf.common.PdfSummaryHelper;
import com.nexaerp.pdf.common.PdfTableHelper;
import com.nexaerp.report.ReportService;
import com.nexaerp.report.dto.TrialBalanceResponseDto;
import com.nexaerp.report.dto.TrialBalanceRowDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class TrialBalancePdfGenerator {

    private final ReportService reportService;

    public byte[] generate(String asOfDate) {

        LocalDate date = LocalDate.parse(asOfDate);

        TrialBalanceResponseDto trialBalance =
                reportService.getTrialBalance(date);

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

        PdfHeader.add(doc, "TRIAL BALANCE", "As of " + PdfFormatUtil.date(date));

        addTable(doc, trialBalance);

        addSummary(doc, trialBalance);

        PdfFooter.add(doc);

        doc.close();

        return baos.toByteArray();
    }

    private void addTable(Document doc,
                          TrialBalanceResponseDto trialBalance) {

        Table table = new Table(UnitValue.createPercentArray(
                new float[]{1.2f, 3f, 1.6f, 1.5f, 1.5f}
        )).useAllAvailableWidth();

        table.addHeaderCell(PdfTableHelper.headerCell("Code"));
        table.addHeaderCell(PdfTableHelper.headerCell("Account"));
        table.addHeaderCell(PdfTableHelper.headerCell("Type"));
        table.addHeaderCell(PdfTableHelper.headerCell("Debit"));
        table.addHeaderCell(PdfTableHelper.headerCell("Credit"));

        for (TrialBalanceRowDto row : trialBalance.getRows()) {

            table.addCell(PdfTableHelper.textCell(
                    PdfFormatUtil.text(row.getAccountCode())));

            table.addCell(PdfTableHelper.textCell(
                    PdfFormatUtil.text(row.getAccountName())));

            table.addCell(PdfTableHelper.textCell(
                    row.getAccountType().name()));

            table.addCell(PdfTableHelper.amountCell(
                    PdfFormatUtil.money(row.getDebitBalance())));

            table.addCell(PdfTableHelper.amountCell(
                    PdfFormatUtil.money(row.getCreditBalance())));
        }

        doc.add(table);
    }

    private void addSummary(Document doc,
                            TrialBalanceResponseDto trialBalance) {

        PdfSummaryHelper.addSummary(doc, new String[][]{

                {
                        "Total Debit:",
                        PdfFormatUtil.money(trialBalance.getTotalDebit())
                },

                {
                        "Total Credit:",
                        PdfFormatUtil.money(trialBalance.getTotalCredit())
                },

                {
                        "Balanced:",
                        trialBalance.getIsBalanced() ? "YES" : "NO",
                        "bold"
                }
        });
    }
}