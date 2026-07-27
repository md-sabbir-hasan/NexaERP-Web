package com.nexaerp.report;

import com.nexaerp.party.PartyType;
import com.nexaerp.account.AccountType;
import com.nexaerp.report.dto.*;

import java.time.LocalDate;

public interface ReportService {
//    Ledger + Trial Balance
    LedgerResponseDto getLedger(Long accountId, LocalDate fromDate, LocalDate toDate);
    TrialBalanceResponseDto getTrialBalance(LocalDate asOfDate);

//    P&L Balance Sheet
    ProfitLossResponseDto getProfitLoss(LocalDate fromDate, LocalDate toDate);
    BalanceSheetResponseDto getBalanceSheet(LocalDate asOfDate);

//    PartyStatement+ Aging

    PartyStatementResponseDto getPartyStatement(Long partyId, LocalDate fromDate, LocalDate toDate);

    AgingResponseDto getAgingReport(PartyType partyType, LocalDate asOfDate);
    CashFlowStatementResponseDto getCashFlowStatement(LocalDate fromDate, LocalDate toDate);
    CostCenterTransactionReportDto getCostCenterTransactions(
            Long costCenterId, LocalDate fromDate, LocalDate toDate);
    BudgetVsActualResponseDto getBudgetVsActual(
            Long budgetId, Long fromPeriodId, Long toPeriodId, AccountType accountType);
}
