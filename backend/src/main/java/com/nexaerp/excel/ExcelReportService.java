package com.nexaerp.excel;

import com.nexaerp.party.PartyType;
import com.nexaerp.report.ReportService;
import com.nexaerp.report.BudgetVsActualReportService;
import com.nexaerp.account.AccountType;
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
    private final BudgetVsActualReportService budgetVsActualReportService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public byte[] generateBudgetVsActualExcel(
            Long budgetId, Long fromPeriodId, Long toPeriodId, AccountType accountType) {
        BudgetVsActualResponseDto data = budgetVsActualReportService
                .generate(budgetId, fromPeriodId, toPeriodId, accountType);
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Budget vs Actual");
            CellStyle title = ExcelStyleHelper.titleStyle(wb);
            CellStyle subtitle = ExcelStyleHelper.subTitleStyle(wb);
            CellStyle header = ExcelStyleHelper.headerStyle(wb);
            CellStyle currency = ExcelStyleHelper.currencyStyle(wb);
            CellStyle boldCurrency = ExcelStyleHelper.boldCurrencyStyle(wb);
            CellStyle percentage = wb.createCellStyle();
            percentage.setDataFormat(wb.createDataFormat().getFormat("0.00%"));
            int row = 0;
            setCell(sheet, row++, 0, "NexaERP - Budget vs Actual Report", title);
            setCell(sheet, row++, 0, data.getBudgetName() + " (" + data.getBudgetStatus() + ")", subtitle);
            setCell(sheet, row++, 0, data.getFiscalYearName() + " | "
                    + data.getFromDate().format(DATE_FMT) + " to " + data.getToDate().format(DATE_FMT), subtitle);
            setCell(sheet, row++, 0, "Generated " + data.getGeneratedAt()
                    + " | Currency " + data.getCurrencyCode(), subtitle);
            row++;
            row = writeBudgetSection(sheet, row, "Revenue", data.getRevenueLines(), header,
                    currency, percentage, boldCurrency);
            row++;
            row = writeBudgetSection(sheet, row, "Expense", data.getExpenseLines(), header,
                    currency, percentage, boldCurrency);
            row++;
            setCell(sheet, row++, 0, "Combined Summary", title);
            setCell(sheet, row, 0, "Revenue Budget", null);
            setNumeric(sheet, row++, 1, data.getTotalRevenueBudget(), boldCurrency);
            setCell(sheet, row, 0, "Revenue Actual", null);
            setNumeric(sheet, row++, 1, data.getTotalRevenueActual(), boldCurrency);
            setCell(sheet, row, 0, "Expense Budget", null);
            setNumeric(sheet, row++, 1, data.getTotalExpenseBudget(), boldCurrency);
            setCell(sheet, row, 0, "Expense Actual", null);
            setNumeric(sheet, row, 1, data.getTotalExpenseActual(), boldCurrency);
            sheet.createFreezePane(0, 7);
            ExcelStyleHelper.autoSizeColumns(sheet, 10);
            return ExcelStyleHelper.toBytes(wb);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Budget vs Actual Excel", e);
        }
    }

    private int writeBudgetSection(Sheet sheet, int row, String label,
                                   java.util.List<BudgetVsActualLineDto> lines,
                                   CellStyle header, CellStyle currency,
                                   CellStyle percentage, CellStyle total) {
        setCell(sheet, row++, 0, label, ExcelStyleHelper.boldStyle(sheet.getWorkbook()));
        String[] headings = {"Account Code", "Account Name", "Type", "Budget", "Actual",
                "Variance", "Variance %", "Achievement/Utilization %", "Remaining", "Status"};
        for (int i = 0; i < headings.length; i++) setCell(sheet, row, i, headings[i], header);
        row++;
        BigDecimal budget = BigDecimal.ZERO;
        BigDecimal actual = BigDecimal.ZERO;
        BigDecimal variance = BigDecimal.ZERO;
        for (BudgetVsActualLineDto line : lines) {
            setCell(sheet, row, 0, line.getAccountCode(), null);
            setCell(sheet, row, 1, line.getAccountName(), null);
            setCell(sheet, row, 2, line.getAccountType().name(), null);
            setNumeric(sheet, row, 3, line.getBudgetAmount(), currency);
            setNumeric(sheet, row, 4, line.getActualAmount(), currency);
            setNumeric(sheet, row, 5, line.getVarianceAmount(), currency);
            setPercent(sheet, row, 6, line.getVariancePercent(), percentage);
            setPercent(sheet, row, 7, line.getUtilizationPercent(), percentage);
            setNumeric(sheet, row, 8, line.getRemainingAmount(), currency);
            setCell(sheet, row++, 9, line.getVarianceStatus().name(), null);
            budget = budget.add(line.getBudgetAmount());
            actual = actual.add(line.getActualAmount());
            variance = variance.add(line.getVarianceAmount());
        }
        setCell(sheet, row, 0, "Total " + label, total);
        setNumeric(sheet, row, 3, budget, total);
        setNumeric(sheet, row, 4, actual, total);
        setNumeric(sheet, row++, 5, variance, total);
        return row;
    }

    private void setPercent(Sheet sheet, int row, int column, BigDecimal value, CellStyle style) {
        if (value == null) {
            setCell(sheet, row, column, "—", null);
        } else {
            setNumeric(sheet, row, column, value.divide(BigDecimal.valueOf(100)), style);
        }
    }

    public byte[] generateCashFlowExcel(LocalDate fromDate, LocalDate toDate) {
        CashFlowStatementResponseDto data = reportService.getCashFlowStatement(fromDate, toDate);
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Cash Flow");
            CellStyle title = ExcelStyleHelper.titleStyle(wb);
            CellStyle subTitle = ExcelStyleHelper.subTitleStyle(wb);
            CellStyle header = ExcelStyleHelper.headerStyle(wb);
            CellStyle currency = ExcelStyleHelper.currencyStyle(wb);
            CellStyle boldCurrency = ExcelStyleHelper.boldCurrencyStyle(wb);
            CellStyle bold = ExcelStyleHelper.boldStyle(wb);
            int r = 0;
            setCell(sheet, r++, 0, "Cash Flow Statement", title);
            setCell(sheet, r++, 0, data.getFromDate().format(DATE_FMT) + " to " + data.getToDate().format(DATE_FMT), subTitle);
            setCell(sheet, r++, 0, "Generated " + data.getGeneratedAt() + " | Currency " + data.getCurrencyCode(), subTitle);
            r++;
            r = writeCashFlowSection(sheet, r, data.getOperatingActivities(), header, currency, bold, boldCurrency);
            r = writeCashFlowSection(sheet, r, data.getInvestingActivities(), header, currency, bold, boldCurrency);
            r = writeCashFlowSection(sheet, r, data.getFinancingActivities(), header, currency, bold, boldCurrency);
            Row opening = sheet.createRow(r++); setCellStyled(opening, 0, "Opening Cash", bold); setCurrencyCell(opening, 3, data.getOpeningCashBalance(), boldCurrency);
            Row change = sheet.createRow(r++); setCellStyled(change, 0, "Net Change in Cash", bold); setCurrencyCell(change, 3, data.getNetChangeInCash(), boldCurrency);
            Row calculated = sheet.createRow(r++); setCellStyled(calculated, 0, "Calculated Closing Cash", bold); setCurrencyCell(calculated, 3, data.getCalculatedClosingCashBalance(), boldCurrency);
            Row ledger = sheet.createRow(r++); setCellStyled(ledger, 0, "Ledger Closing Cash", bold); setCurrencyCell(ledger, 3, data.getLedgerClosingCashBalance(), boldCurrency);
            Row reconciliation = sheet.createRow(r++); setCellStyled(reconciliation, 0, "Reconciliation", bold);
            setCellStyled(reconciliation, 2, Boolean.TRUE.equals(data.getIsReconciled()) ? "Reconciled" : "Difference", bold);
            setCurrencyCell(reconciliation, 3, data.getReconciliationDifference(), boldCurrency);
            r += 2;
            setCellStyled(sheet.createRow(r++), 0, "Cash Account Breakdown", bold);
            writeHeaderRow(sheet, r++, new String[]{"Code", "Account", "Opening", "Period Movement", "Closing"}, header);
            for (CashFlowAccountBalanceDto account : data.getCashAccounts()) {
                Row row = sheet.createRow(r++); row.createCell(0).setCellValue(account.getAccountCode()); row.createCell(1).setCellValue(account.getAccountName());
                setCurrencyCell(row, 2, account.getOpeningBalance(), currency); setCurrencyCell(row, 3, account.getPeriodMovement(), currency); setCurrencyCell(row, 4, account.getClosingBalance(), currency);
            }
            if (!data.getUnclassifiedMovements().isEmpty()) {
                r += 2; setCellStyled(sheet.createRow(r++), 0, "Unclassified Movement Warnings", bold);
                writeHeaderRow(sheet, r++, new String[]{"Date", "Journal", "Description", "Amount", "Reason"}, header);
                for (UnclassifiedCashMovementDto warning : data.getUnclassifiedMovements()) {
                    Row row = sheet.createRow(r++); row.createCell(0).setCellValue(warning.getDate().format(DATE_FMT)); row.createCell(1).setCellValue(warning.getEntryNumber());
                    row.createCell(2).setCellValue(nvl(warning.getDescription())); setCurrencyCell(row, 3, warning.getAmount(), currency); row.createCell(4).setCellValue(warning.getReason());
                }
            }
            ExcelStyleHelper.autoSizeColumns(sheet, 5);
            return ExcelStyleHelper.toBytes(wb);
        } catch (Exception e) { throw new RuntimeException("Failed to generate cash flow Excel", e); }
    }

    private int writeCashFlowSection(Sheet sheet, int r, CashFlowActivitySectionDto section,
                                     CellStyle header, CellStyle currency, CellStyle bold, CellStyle boldCurrency) {
        setCellStyled(sheet.createRow(r++), 0, section.getActivity().name() + " ACTIVITIES", bold);
        writeHeaderRow(sheet, r++, new String[]{"Description", "Inflow", "Outflow", "Net"}, header);
        for (CashFlowLineItemDto item : section.getItems()) {
            Row row = sheet.createRow(r++); row.createCell(0).setCellValue(item.getLabel());
            setCurrencyCell(row, 1, item.getInflow(), currency); setCurrencyCell(row, 2, item.getOutflow(), currency); setCurrencyCell(row, 3, item.getNetAmount(), currency);
        }
        Row total = sheet.createRow(r++); setCellStyled(total, 0, "Net " + section.getActivity().name().toLowerCase() + " cash flow", bold);
        setCurrencyCell(total, 1, section.getTotalInflows(), boldCurrency); setCurrencyCell(total, 2, section.getTotalOutflows(), boldCurrency); setCurrencyCell(total, 3, section.getNetCashFlow(), boldCurrency);
        return r + 1;
    }

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

    private void setNumeric(Sheet sheet, int rowIndex, int colIndex, BigDecimal value, CellStyle style) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) row = sheet.createRow(rowIndex);
        setCurrencyCell(row, colIndex, value, style);
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
