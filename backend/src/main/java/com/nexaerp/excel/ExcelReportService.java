package com.nexaerp.excel;

import com.nexaerp.party.PartyType;
import com.nexaerp.report.ReportService;
import com.nexaerp.report.dto.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class ExcelReportService {

    private final ReportService reportService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    // ==================== Ledger ====================

    public byte[] generateLedgerExcel(Long accountId, LocalDate fromDate, LocalDate toDate) {
        LedgerResponseDto data = reportService.getLedger(accountId, fromDate, toDate);

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Ledger");
            CellStyle title = ExcelStyleHelper.titleStyle(wb);
            CellStyle subTitle = ExcelStyleHelper.subTitleStyle(wb);
            CellStyle header = ExcelStyleHelper.headerStyle(wb);
            CellStyle currency = ExcelStyleHelper.currencyStyle(wb);
            CellStyle boldCurrency = ExcelStyleHelper.boldCurrencyStyle(wb);
            CellStyle bold = ExcelStyleHelper.boldStyle(wb);

            int r = 0;
            setCell(sheet, r++, 0, data.getAccountCode() + " - " + data.getAccountName(), title);
            setCell(sheet, r++, 0, "Ledger Report  |  " + data.getFromDate().format(DATE_FMT)
                    + " to " + data.getToDate().format(DATE_FMT), subTitle);
            r++;

            String[] headers = {"Date", "Journal No.", "Reference", "Description", "Debit", "Credit", "Balance"};
            writeHeaderRow(sheet, r++, headers, header);

            for (LedgerEntryDto entry : data.getEntries()) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(entry.getDate() != null ? entry.getDate().format(DATE_FMT) : "");
                row.createCell(1).setCellValue(nvl(entry.getJournalEntryNumber()));
                row.createCell(2).setCellValue(nvl(entry.getReferenceNumber()));
                row.createCell(3).setCellValue(nvl(entry.getDescription()));
                setCurrencyCell(row, 4, entry.getDebit(), currency);
                setCurrencyCell(row, 5, entry.getCredit(), currency);
                setCurrencyCell(row, 6, entry.getRunningBalance(), currency);
            }

            r++;
            Row totalRow = sheet.createRow(r);
            setCellStyled(totalRow, 3, "Totals", bold);
            setCurrencyCell(totalRow, 4, data.getTotalDebit(), boldCurrency);
            setCurrencyCell(totalRow, 5, data.getTotalCredit(), boldCurrency);
            setCurrencyCell(totalRow, 6, data.getClosingBalance(), boldCurrency);

            ExcelStyleHelper.autoSizeColumns(sheet, headers.length);
            return ExcelStyleHelper.toBytes(wb);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate ledger Excel", e);
        }
    }

    // ==================== Trial Balance ====================

    public byte[] generateTrialBalanceExcel(LocalDate asOfDate) {
        TrialBalanceResponseDto data = reportService.getTrialBalance(asOfDate);

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Trial Balance");
            CellStyle title = ExcelStyleHelper.titleStyle(wb);
            CellStyle subTitle = ExcelStyleHelper.subTitleStyle(wb);
            CellStyle header = ExcelStyleHelper.headerStyle(wb);
            CellStyle currency = ExcelStyleHelper.currencyStyle(wb);
            CellStyle boldCurrency = ExcelStyleHelper.boldCurrencyStyle(wb);
            CellStyle bold = ExcelStyleHelper.boldStyle(wb);

            int r = 0;
            setCell(sheet, r++, 0, "Trial Balance", title);
            setCell(sheet, r++, 0, "As of " + data.getAsOfDate().format(DATE_FMT), subTitle);
            r++;

            String[] headers = {"Code", "Account Name", "Type", "Debit", "Credit"};
            writeHeaderRow(sheet, r++, headers, header);

            for (TrialBalanceRowDto row : data.getRows()) {
                Row excelRow = sheet.createRow(r++);
                excelRow.createCell(0).setCellValue(nvl(row.getAccountCode()));
                excelRow.createCell(1).setCellValue(nvl(row.getAccountName()));
                excelRow.createCell(2).setCellValue(row.getAccountType() != null ? row.getAccountType().name() : "");
                setCurrencyCell(excelRow, 3, row.getDebitBalance(), currency);
                setCurrencyCell(excelRow, 4, row.getCreditBalance(), currency);
            }

            r++;
            Row totalRow = sheet.createRow(r++);
            setCellStyled(totalRow, 1, "Totals", bold);
            setCurrencyCell(totalRow, 3, data.getTotalDebit(), boldCurrency);
            setCurrencyCell(totalRow, 4, data.getTotalCredit(), boldCurrency);

            Row statusRow = sheet.createRow(r);
            setCellStyled(statusRow, 1, Boolean.TRUE.equals(data.getIsBalanced()) ? "Balanced ✔" : "NOT BALANCED", bold);

            ExcelStyleHelper.autoSizeColumns(sheet, headers.length);
            return ExcelStyleHelper.toBytes(wb);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate trial balance Excel", e);
        }
    }

    // ==================== Profit & Loss ====================

    public byte[] generateProfitLossExcel(LocalDate fromDate, LocalDate toDate) {
        ProfitLossResponseDto data = reportService.getProfitLoss(fromDate, toDate);

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Profit and Loss");
            CellStyle title = ExcelStyleHelper.titleStyle(wb);
            CellStyle subTitle = ExcelStyleHelper.subTitleStyle(wb);
            CellStyle header = ExcelStyleHelper.headerStyle(wb);
            CellStyle currency = ExcelStyleHelper.currencyStyle(wb);
            CellStyle boldCurrency = ExcelStyleHelper.boldCurrencyStyle(wb);
            CellStyle bold = ExcelStyleHelper.boldStyle(wb);

            int r = 0;
            setCell(sheet, r++, 0, "Profit & Loss Statement", title);
            setCell(sheet, r++, 0, data.getFromDate().format(DATE_FMT) + " to " + data.getToDate().format(DATE_FMT), subTitle);
            r++;

            setCellStyled(sheet.createRow(r++), 0, "Revenue", bold);
            String[] headers = {"Code", "Account Name", "Amount"};
            writeHeaderRow(sheet, r++, headers, header);
            for (ProfitLossRowDto row : data.getRevenues()) {
                Row excelRow = sheet.createRow(r++);
                excelRow.createCell(0).setCellValue(nvl(row.getAccountCode()));
                excelRow.createCell(1).setCellValue(nvl(row.getAccountName()));
                setCurrencyCell(excelRow, 2, row.getAmount(), currency);
            }
            Row totalRevenueRow = sheet.createRow(r++);
            setCellStyled(totalRevenueRow, 1, "Total Revenue", bold);
            setCurrencyCell(totalRevenueRow, 2, data.getTotalRevenue(), boldCurrency);
            r++;

            setCellStyled(sheet.createRow(r++), 0, "Expenses", bold);
            writeHeaderRow(sheet, r++, headers, header);
            for (ProfitLossRowDto row : data.getExpenses()) {
                Row excelRow = sheet.createRow(r++);
                excelRow.createCell(0).setCellValue(nvl(row.getAccountCode()));
                excelRow.createCell(1).setCellValue(nvl(row.getAccountName()));
                setCurrencyCell(excelRow, 2, row.getAmount(), currency);
            }
            Row totalExpenseRow = sheet.createRow(r++);
            setCellStyled(totalExpenseRow, 1, "Total Expense", bold);
            setCurrencyCell(totalExpenseRow, 2, data.getTotalExpense(), boldCurrency);
            r++;

            Row netProfitRow = sheet.createRow(r);
            setCellStyled(netProfitRow, 1, "Net Profit", bold);
            setCurrencyCell(netProfitRow, 2, data.getNetProfit(), boldCurrency);

            ExcelStyleHelper.autoSizeColumns(sheet, headers.length);
            return ExcelStyleHelper.toBytes(wb);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate profit & loss Excel", e);
        }
    }

    // ==================== Balance Sheet ====================

    public byte[] generateBalanceSheetExcel(LocalDate asOfDate) {
        BalanceSheetResponseDto data = reportService.getBalanceSheet(asOfDate);

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Balance Sheet");
            CellStyle title = ExcelStyleHelper.titleStyle(wb);
            CellStyle subTitle = ExcelStyleHelper.subTitleStyle(wb);
            CellStyle header = ExcelStyleHelper.headerStyle(wb);
            CellStyle currency = ExcelStyleHelper.currencyStyle(wb);
            CellStyle boldCurrency = ExcelStyleHelper.boldCurrencyStyle(wb);
            CellStyle bold = ExcelStyleHelper.boldStyle(wb);

            int r = 0;
            setCell(sheet, r++, 0, "Balance Sheet", title);
            setCell(sheet, r++, 0, "As of " + data.getAsOfDate().format(DATE_FMT), subTitle);
            r++;

            String[] headers = {"Code", "Account Name", "Amount"};

            setCellStyled(sheet.createRow(r++), 0, "Assets", bold);
            writeHeaderRow(sheet, r++, headers, header);
            for (BalanceSheetRowDto row : data.getAssets()) {
                r = writeBsRow(sheet, r, row, currency);
            }
            Row totalAssetsRow = sheet.createRow(r++);
            setCellStyled(totalAssetsRow, 1, "Total Assets", bold);
            setCurrencyCell(totalAssetsRow, 2, data.getTotalAssets(), boldCurrency);
            r++;

            setCellStyled(sheet.createRow(r++), 0, "Liabilities", bold);
            writeHeaderRow(sheet, r++, headers, header);
            for (BalanceSheetRowDto row : data.getLiabilities()) {
                r = writeBsRow(sheet, r, row, currency);
            }
            Row totalLiabRow = sheet.createRow(r++);
            setCellStyled(totalLiabRow, 1, "Total Liabilities", bold);
            setCurrencyCell(totalLiabRow, 2, data.getTotalLiabilities(), boldCurrency);
            r++;

            setCellStyled(sheet.createRow(r++), 0, "Equity", bold);
            writeHeaderRow(sheet, r++, headers, header);
            for (BalanceSheetRowDto row : data.getEquity()) {
                r = writeBsRow(sheet, r, row, currency);
            }
            Row netProfitRow = sheet.createRow(r++);
            setCellStyled(netProfitRow, 1, "Net Profit (current period)", bold);
            setCurrencyCell(netProfitRow, 2, data.getNetProfit(), currency);
            Row totalEquityRow = sheet.createRow(r++);
            setCellStyled(totalEquityRow, 1, "Total Equity", bold);
            setCurrencyCell(totalEquityRow, 2, data.getTotalEquity(), boldCurrency);
            r++;

            Row totalLERow = sheet.createRow(r++);
            setCellStyled(totalLERow, 1, "Total Liabilities + Equity", bold);
            setCurrencyCell(totalLERow, 2, data.getTotalLiabilitiesAndEquity(), boldCurrency);

            Row statusRow = sheet.createRow(r);
            setCellStyled(statusRow, 1, Boolean.TRUE.equals(data.getIsBalanced()) ? "Balanced ✔" : "NOT BALANCED", bold);

            ExcelStyleHelper.autoSizeColumns(sheet, headers.length);
            return ExcelStyleHelper.toBytes(wb);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate balance sheet Excel", e);
        }
    }

    private int writeBsRow(Sheet sheet, int r, BalanceSheetRowDto row, CellStyle currency) {
        Row excelRow = sheet.createRow(r);
        excelRow.createCell(0).setCellValue(nvl(row.getAccountCode()));
        excelRow.createCell(1).setCellValue(nvl(row.getAccountName()));
        setCurrencyCell(excelRow, 2, row.getAmount(), currency);
        return r + 1;
    }

    // ==================== Party Statement ====================

    public byte[] generatePartyStatementExcel(Long partyId, LocalDate fromDate, LocalDate toDate) {
        PartyStatementResponseDto data = reportService.getPartyStatement(partyId, fromDate, toDate);

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Party Statement");
            CellStyle title = ExcelStyleHelper.titleStyle(wb);
            CellStyle subTitle = ExcelStyleHelper.subTitleStyle(wb);
            CellStyle header = ExcelStyleHelper.headerStyle(wb);
            CellStyle currency = ExcelStyleHelper.currencyStyle(wb);
            CellStyle boldCurrency = ExcelStyleHelper.boldCurrencyStyle(wb);
            CellStyle bold = ExcelStyleHelper.boldStyle(wb);

            int r = 0;
            setCell(sheet, r++, 0, data.getPartyName() + " (" + data.getPartyType() + ")", title);
            setCell(sheet, r++, 0, data.getFromDate().format(DATE_FMT) + " to " + data.getToDate().format(DATE_FMT), subTitle);
            r++;

            Row openingRow = sheet.createRow(r++);
            setCellStyled(openingRow, 3, "Opening Balance", bold);
            setCurrencyCell(openingRow, 4, data.getOpeningBalance(), boldCurrency);
            r++;

            String[] headers = {"Date", "Type", "Reference", "Description", "Debit", "Credit", "Balance"};
            writeHeaderRow(sheet, r++, headers, header);

            for (PartyStatementEntryDto entry : data.getEntries()) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(entry.getDate() != null ? entry.getDate().format(DATE_FMT) : "");
                row.createCell(1).setCellValue(entry.getType() != null ? entry.getType().name() : "");
                row.createCell(2).setCellValue(nvl(entry.getReferenceNumber()));
                row.createCell(3).setCellValue(nvl(entry.getDescription()));
                setCurrencyCell(row, 4, entry.getDebit(), currency);
                setCurrencyCell(row, 5, entry.getCredit(), currency);
                setCurrencyCell(row, 6, entry.getRunningBalance(), currency);
            }

            r++;
            Row closingRow = sheet.createRow(r);
            setCellStyled(closingRow, 3, "Closing Balance", bold);
            setCurrencyCell(closingRow, 6, data.getClosingBalance(), boldCurrency);

            ExcelStyleHelper.autoSizeColumns(sheet, headers.length);
            return ExcelStyleHelper.toBytes(wb);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate party statement Excel", e);
        }
    }

    // ==================== Aging ====================

    public byte[] generateAgingExcel(PartyType partyType, LocalDate asOfDate) {
        AgingResponseDto data = reportService.getAgingReport(partyType, asOfDate);

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Aging Report");
            CellStyle title = ExcelStyleHelper.titleStyle(wb);
            CellStyle subTitle = ExcelStyleHelper.subTitleStyle(wb);
            CellStyle header = ExcelStyleHelper.headerStyle(wb);
            CellStyle currency = ExcelStyleHelper.currencyStyle(wb);
            CellStyle boldCurrency = ExcelStyleHelper.boldCurrencyStyle(wb);
            CellStyle bold = ExcelStyleHelper.boldStyle(wb);

            int r = 0;
            setCell(sheet, r++, 0, "Aging Report - " + data.getPartyType(), title);
            setCell(sheet, r++, 0, "As of " + data.getAsOfDate().format(DATE_FMT), subTitle);
            r++;

            String[] headers = {"Party", "Current", "1-30 Days", "31-60 Days", "61-90 Days", "91+ Days", "Total Due"};
            writeHeaderRow(sheet, r++, headers, header);

            for (AgingRowDto row : data.getRows()) {
                Row excelRow = sheet.createRow(r++);
                excelRow.createCell(0).setCellValue(nvl(row.getPartyName()));
                setCurrencyCell(excelRow, 1, row.getCurrent(), currency);
                setCurrencyCell(excelRow, 2, row.getDays1to30(), currency);
                setCurrencyCell(excelRow, 3, row.getDays31to60(), currency);
                setCurrencyCell(excelRow, 4, row.getDays61to90(), currency);
                setCurrencyCell(excelRow, 5, row.getDays91Plus(), currency);
                setCurrencyCell(excelRow, 6, row.getTotalDue(), currency);
            }

            r++;
            Row totalRow = sheet.createRow(r);
            setCellStyled(totalRow, 0, "Total", bold);
            setCurrencyCell(totalRow, 6, data.getTotalDue(), boldCurrency);

            ExcelStyleHelper.autoSizeColumns(sheet, headers.length);
            return ExcelStyleHelper.toBytes(wb);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate aging report Excel", e);
        }
    }

    // ==================== helpers ====================

    private void writeHeaderRow(Sheet sheet, int rowIndex, String[] headers, CellStyle style) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private void setCell(Sheet sheet, int rowIndex, int colIndex, String value, CellStyle style) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) row = sheet.createRow(rowIndex);
        setCellStyled(row, colIndex, value, style);
    }

    private void setCellStyled(Row row, int colIndex, String value, CellStyle style) {
        Cell cell = row.createCell(colIndex);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void setCurrencyCell(Row row, int colIndex, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(colIndex);
        cell.setCellValue(value != null ? value.doubleValue() : 0d);
        cell.setCellStyle(style);
    }

    private String nvl(String value) {
        return value != null ? value : "";
    }
}