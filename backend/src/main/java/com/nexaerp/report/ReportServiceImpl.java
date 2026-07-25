package com.nexaerp.report;


import com.nexaerp.account.Account;
import com.nexaerp.account.AccountRepository;
import com.nexaerp.account.AccountType;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.invoice.Invoice;
import com.nexaerp.invoice.InvoiceRepository;
import com.nexaerp.invoice.InvoiceStatus;
import com.nexaerp.journal.JournalLine;
import com.nexaerp.journal.JournalLineRepository;
import com.nexaerp.journal.JournalStatus;
import com.nexaerp.party.Party;
import com.nexaerp.party.PartyRepository;
import com.nexaerp.party.PartyType;
import com.nexaerp.payment.Payment;
import com.nexaerp.payment.PaymentRepository;
import com.nexaerp.payment.PaymentType;
import com.nexaerp.report.dto.*;
import com.nexaerp.vendorbill.VendorBill;
import com.nexaerp.vendorbill.VendorBillRepository;
import com.nexaerp.vendorbill.VendorBillStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService{

    private final AccountRepository accountRepository;
    private final JournalLineRepository journalLineRepository;
    private final InvoiceRepository invoiceRepository;
    private final VendorBillRepository vendorBillRepository;
    private final PaymentRepository paymentRepository;
    private final PartyRepository partyRepository;



    @Override
    public LedgerResponseDto getLedger(Long accountId, LocalDate fromDate, LocalDate toDate) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        List<JournalStatus> reportStatuses = List.of(
                JournalStatus.POSTED,
                JournalStatus.REVERSED
        );

        // calculate opening balance from all lines BEFORE fromDate
        List<JournalLine> beforeLines =
                journalLineRepository.findByAccountIdAndJournalEntry_StatusInAndJournalEntry_DateBefore(
                        accountId,
                        reportStatuses,
                        fromDate
                );

        BigDecimal openingBalance = calculateNetEffect(account, beforeLines);

        // get all lines WITHIN the date range, sorted by date
        List<JournalLine> rangeLines =
                journalLineRepository.findByAccountIdAndJournalEntry_StatusInAndJournalEntry_DateBetweenOrderByJournalEntry_DateAsc(
                        accountId,
                        reportStatuses,
                        fromDate,
                        toDate
                );

        // walk through lines one by one, building running balance
        BigDecimal runningBalance = openingBalance;
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        List<LedgerEntryDto> entries = new java.util.ArrayList<>();

        for (JournalLine line : rangeLines) {

            // Debit/Credit affect balance differently depending on account type
            runningBalance = applySingleLineEffect(account, runningBalance, line);

            totalDebit = totalDebit.add(line.getDebit());
            totalCredit = totalCredit.add(line.getCredit());

            entries.add(LedgerEntryDto.builder()
                    .journalEntryId(line.getJournalEntry().getId())
                    .date(line.getJournalEntry().getDate())
                    .journalEntryNumber(line.getJournalEntry().getEntryNumber())
                    .sourceType(line.getJournalEntry().getSourceType())
                    .sourceId(line.getJournalEntry().getSourceId())
                    .referenceNumber(line.getJournalEntry().getReferenceNumber())
                    .description(line.getDescription())
                    .debit(line.getDebit())
                    .credit(line.getCredit())
                    .runningBalance(runningBalance)
                    .build());
        }

        return LedgerResponseDto.builder()
                .accountId(account.getId())
                .accountCode(account.getCode())
                .accountName(account.getName())
                .accountType(account.getType())
                .fromDate(fromDate)
                .toDate(toDate)
                .openingBalance(openingBalance)
                .closingBalance(runningBalance)
                .totalDebit(totalDebit)
                .totalCredit(totalCredit)
                .entries(entries)
                .build();
    }

    @Override
    public TrialBalanceResponseDto getTrialBalance(LocalDate asOfDate) {
        // Trial Balance reads current account balances.
        // as of Date is currently unused (reserved for historical reports).
        List<Account> accounts = accountRepository.findAll();

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        List<TrialBalanceRowDto> rows = new java.util.ArrayList<>();

        for (Account account : accounts) {

            BigDecimal balance = account.getCurrentBalance();

            //Get balance side based on account type.
            BigDecimal debitBalance = BigDecimal.ZERO;
            BigDecimal creditBalance = BigDecimal.ZERO;

            boolean isNaturallyDebit =
                    account.getType() == AccountType.ASSET ||
                            account.getType() == AccountType.EXPENSE;

            if (isNaturallyDebit) {
                if (balance.compareTo(BigDecimal.ZERO) >= 0) {
                    debitBalance = balance;
                } else {
                    // Negative balance on a debit-natured account shows on credit side
                    creditBalance = balance.abs();
                }
            } else {
                if (balance.compareTo(BigDecimal.ZERO) >= 0) {
                    creditBalance = balance;
                } else {
                    debitBalance = balance.abs();
                }
            }

            totalDebit = totalDebit.add(debitBalance);
            totalCredit = totalCredit.add(creditBalance);

            rows.add(TrialBalanceRowDto.builder()
                    .accountId(account.getId())
                    .accountCode(account.getCode())
                    .accountName(account.getName())
                    .accountType(account.getType())
                    .debitBalance(debitBalance)
                    .creditBalance(creditBalance)
                    .build());
        }

        // Sort rows by account
        rows.sort(Comparator.comparing(TrialBalanceRowDto::getAccountCode));

        return TrialBalanceResponseDto.builder()
                .asOfDate(asOfDate)
                .rows(rows)
                .totalDebit(totalDebit)
                .totalCredit(totalCredit)
                .isBalanced(totalDebit.compareTo(totalCredit) == 0)
                .build();
    }

    @Override
    public ProfitLossResponseDto getProfitLoss(
            LocalDate fromDate,
            LocalDate toDate
    ) {
        if (fromDate == null || toDate == null) {
            throw new BusinessRuleException(
                    "From date and to date are required"
            );
        }

        if (fromDate.isAfter(toDate)) {
            throw new BusinessRuleException(
                    "From date cannot be after to date"
            );
        }

        List<Account> revenueAccounts = accountRepository
                .findByType(AccountType.REVENUE)
                .stream()
                .filter(this::isLeafAccount)
                .sorted(Comparator.comparing(Account::getCode))
                .toList();

        List<Account> expenseAccounts = accountRepository
                .findByType(AccountType.EXPENSE)
                .stream()
                .filter(this::isLeafAccount)
                .sorted(Comparator.comparing(Account::getCode))
                .toList();

        List<ProfitLossRowDto> revenues = new java.util.ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (Account account : revenueAccounts) {
            BigDecimal amount = calculatePeriodBalance(
                    account,
                    fromDate,
                    toDate
            );

            if (amount.compareTo(BigDecimal.ZERO) != 0) {
                revenues.add(
                        ProfitLossRowDto.builder()
                                .accountId(account.getId())
                                .accountCode(account.getCode())
                                .accountName(account.getName())
                                .amount(amount)
                                .build()
                );

                totalRevenue = totalRevenue.add(amount);
            }
        }

        List<ProfitLossRowDto> expenses = new java.util.ArrayList<>();
        BigDecimal totalExpense = BigDecimal.ZERO;

        for (Account account : expenseAccounts) {
            BigDecimal amount = calculatePeriodBalance(
                    account,
                    fromDate,
                    toDate
            );

            if (amount.compareTo(BigDecimal.ZERO) != 0) {
                expenses.add(
                        ProfitLossRowDto.builder()
                                .accountId(account.getId())
                                .accountCode(account.getCode())
                                .accountName(account.getName())
                                .amount(amount)
                                .build()
                );

                totalExpense = totalExpense.add(amount);
            }
        }

        return ProfitLossResponseDto.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .revenues(revenues)
                .totalRevenue(totalRevenue)
                .expenses(expenses)
                .totalExpense(totalExpense)
                .netProfit(totalRevenue.subtract(totalExpense))
                .build();
    }

    @Override
    public BalanceSheetResponseDto getBalanceSheet(LocalDate asOfDate) {
        List<Account> assetAccounts = accountRepository.findByType(AccountType.ASSET);
        List<Account> liabilityAccounts = accountRepository.findByType(AccountType.LIABILITY);
        List<Account> equityAccounts = accountRepository.findByType(AccountType.EQUITY);

        // Assets — only leaf accounts (no children) to avoid double counting
        List<BalanceSheetRowDto> assets = new java.util.ArrayList<>();
        BigDecimal totalAssets = BigDecimal.ZERO;

        for (Account account : assetAccounts) {
            if (account.getChildren() == null || account.getChildren().isEmpty()) {
                BigDecimal balance = account.getCurrentBalance();
                if (balance.compareTo(BigDecimal.ZERO) != 0) {
                    assets.add(BalanceSheetRowDto.builder()
                            .accountId(account.getId())
                            .accountCode(account.getCode())
                            .accountName(account.getName())
                            .amount(balance)
                            .build());
                    totalAssets = totalAssets.add(balance);
                }
            }
        }

        // Liabilities
        List<BalanceSheetRowDto> liabilities = new java.util.ArrayList<>();
        BigDecimal totalLiabilities = BigDecimal.ZERO;

        for (Account account : liabilityAccounts) {
            if (account.getChildren() == null || account.getChildren().isEmpty()) {
                BigDecimal balance = account.getCurrentBalance();
                if (balance.compareTo(BigDecimal.ZERO) != 0) {
                    liabilities.add(BalanceSheetRowDto.builder()
                            .accountId(account.getId())
                            .accountCode(account.getCode())
                            .accountName(account.getName())
                            .amount(balance)
                            .build());
                    totalLiabilities = totalLiabilities.add(balance);
                }
            }
        }

        // Equity (excluding current period profit, that's added separately)
        List<BalanceSheetRowDto> equity = new java.util.ArrayList<>();
        BigDecimal totalEquityExcludingProfit = BigDecimal.ZERO;

        for (Account account : equityAccounts) {
            if (account.getChildren() == null || account.getChildren().isEmpty()) {
                BigDecimal balance = account.getCurrentBalance();
                if (balance.compareTo(BigDecimal.ZERO) != 0) {
                    equity.add(BalanceSheetRowDto.builder()
                            .accountId(account.getId())
                            .accountCode(account.getCode())
                            .accountName(account.getName())
                            .amount(balance)
                            .build());
                    totalEquityExcludingProfit = totalEquityExcludingProfit.add(balance);
                }
            }
        }

        // Net profit till date (Revenue - Expense), added into equity
        BigDecimal totalRevenue = accountRepository.findByType(AccountType.REVENUE)
                .stream()
                .map(Account::getCurrentBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = accountRepository.findByType(AccountType.EXPENSE)
                .stream()
                .map(Account::getCurrentBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netProfit = totalRevenue.subtract(totalExpense);

        BigDecimal totalEquity = totalEquityExcludingProfit.add(netProfit);
        BigDecimal totalLiabilitiesAndEquity = totalLiabilities.add(totalEquity);

        return BalanceSheetResponseDto.builder()
                .asOfDate(asOfDate)
                .assets(assets)
                .totalAssets(totalAssets)
                .liabilities(liabilities)
                .totalLiabilities(totalLiabilities)
                .equity(equity)
                .totalEquityExcludingProfit(totalEquityExcludingProfit)
                .netProfit(netProfit)
                .totalEquity(totalEquity)
                .totalLiabilitiesAndEquity(totalLiabilitiesAndEquity)
                .isBalanced(totalAssets.compareTo(totalLiabilitiesAndEquity) == 0)
                .build();
    }


    // =========Party Statement Method============
    @Override
    public PartyStatementResponseDto getPartyStatement(Long partyId, LocalDate fromDate, LocalDate toDate) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new ResourceNotFoundException("Party not found"));

        // Step 1 — Opening balance: net effect of everything BEFORE fromDate
        BigDecimal openingBalance = calculatePartyBalanceBefore(partyId, party.getType(), fromDate);

        // Step 2 — Collect all entries within the date range from Invoice, VendorBill, Payment
        List<PartyStatementEntryDto> rawEntries = new java.util.ArrayList<>();

        // Invoices (only for CUSTOMER/BOTH)
        invoiceRepository.findByPartyId(partyId).stream()
                .filter(inv -> inv.getStatus() != InvoiceStatus.CANCELLED)
                .filter(inv -> !inv.getInvoiceDate().isBefore(fromDate) && !inv.getInvoiceDate().isAfter(toDate))
                .forEach(inv -> rawEntries.add(PartyStatementEntryDto.builder()
                        .date(inv.getInvoiceDate())
                        .type(StatementEntryType.SALES_INVOICE)
                        .referenceId(inv.getId())
                        .referenceNumber(inv.getInvoiceNumber())
                        .description("Sales Invoice")
                        .debit(inv.getGrandTotal())
                        .credit(BigDecimal.ZERO)
                        .build()));

        // Vendor Bills
        vendorBillRepository.findByPartyId(partyId).stream()
                .filter(bill -> bill.getStatus() != com.nexaerp.vendorbill.VendorBillStatus.CANCELLED)
                .filter(bill -> !bill.getBillDate().isBefore(fromDate) && !bill.getBillDate().isAfter(toDate))
                .forEach(bill -> rawEntries.add(PartyStatementEntryDto.builder()
                        .date(bill.getBillDate())
                        .type(StatementEntryType.PURCHASE_BILL)
                        .referenceId(bill.getId())
                        .referenceNumber(bill.getBillNumber())
                        .description("Purchase Bill")
                        .debit(BigDecimal.ZERO)
                        .credit(bill.getNetPayable())
                        .build()));

        // Payments
        paymentRepository.findByPartyId(partyId).stream()
                .filter(p -> p.getStatus() == com.nexaerp.payment.PaymentStatus.POSTED)
                .filter(p -> !p.getPaymentDate().isBefore(fromDate) && !p.getPaymentDate().isAfter(toDate))
                .forEach(p -> {
                    boolean isReceipt = p.getPaymentType() == PaymentType.RECEIPT;
                    rawEntries.add(PartyStatementEntryDto.builder()
                            .date(p.getPaymentDate())
                            .type(isReceipt ? StatementEntryType.RECEIPT : StatementEntryType.PAYMENT)
                            .referenceId(p.getId())
                            .referenceNumber(p.getPaymentNumber())
                            .description(isReceipt ? "Payment Received" : "Payment Made")
                            .debit(isReceipt ? BigDecimal.ZERO : p.getAmount())
                            .credit(isReceipt ? p.getAmount() : BigDecimal.ZERO)
                            .build());
                });

        // Step 3 — Sort by date, then reference number
        rawEntries.sort(java.util.Comparator
                .comparing(PartyStatementEntryDto::getDate)
                .thenComparing(PartyStatementEntryDto::getReferenceNumber));

        // Step 4 — Calculate running balance
        BigDecimal runningBalance = openingBalance;
        List<PartyStatementEntryDto> entries = new java.util.ArrayList<>();

        for (PartyStatementEntryDto entry : rawEntries) {
            runningBalance = runningBalance.add(entry.getDebit()).subtract(entry.getCredit());
            entries.add(PartyStatementEntryDto.builder()
                    .date(entry.getDate())
                    .type(entry.getType())
                    .referenceId(entry.getReferenceId())
                    .referenceNumber(entry.getReferenceNumber())
                    .description(entry.getDescription())
                    .debit(entry.getDebit())
                    .credit(entry.getCredit())
                    .runningBalance(runningBalance)
                    .build());
        }

        return PartyStatementResponseDto.builder()
                .partyId(party.getId())
                .partyName(party.getName())
                .partyType(party.getType().name())
                .fromDate(fromDate)
                .toDate(toDate)
                .openingBalance(openingBalance)
                .entries(entries)
                .closingBalance(runningBalance)
                .build();
    }

    @Override
    public AgingResponseDto getAgingReport(PartyType partyType, LocalDate asOfDate) {
        List<AgingRowDto> rows = new java.util.ArrayList<>();
        BigDecimal grandTotal = BigDecimal.ZERO;

        if (partyType == PartyType.CUSTOMER || partyType == PartyType.BOTH) {

            // Group outstanding invoices by party
            Map<Long, List<Invoice>> invoicesByParty = invoiceRepository.findAll().stream()
                    .filter(inv -> inv.getDueAmount().compareTo(BigDecimal.ZERO) > 0)
                    .filter(inv -> inv.getStatus() != InvoiceStatus.CANCELLED)
                    .collect(Collectors.groupingBy(inv -> inv.getParty().getId()));

            for (Map.Entry<Long, List<Invoice>> entry : invoicesByParty.entrySet()) {

                Party party = partyRepository.findById(entry.getKey()).orElse(null);
                if (party == null) continue;

                AgingRowDto row = buildAgingRowFromInvoices(party, entry.getValue(), asOfDate);
                rows.add(row);
                grandTotal = grandTotal.add(row.getTotalDue());
            }
        }

        if (partyType == PartyType.VENDOR) {

            Map<Long, List<VendorBill>> billsByParty = vendorBillRepository.findAll().stream()
                    .filter(bill -> bill.getDueAmount().compareTo(BigDecimal.ZERO) > 0)
                    .filter(bill -> bill.getStatus() != VendorBillStatus.CANCELLED)
                    .collect(Collectors.groupingBy(bill -> bill.getParty().getId()));

            for (Map.Entry<Long, List<VendorBill>> entry : billsByParty.entrySet()) {

                Party party = partyRepository.findById(entry.getKey()).orElse(null);
                if (party == null) continue;

                AgingRowDto row = buildAgingRowFromBills(party, entry.getValue(), asOfDate);
                rows.add(row);
                grandTotal = grandTotal.add(row.getTotalDue());
            }
        }

        return AgingResponseDto.builder()
                .asOfDate(asOfDate)
                .partyType(partyType.name())
                .rows(rows)
                .totalDue(grandTotal)
                .build();
    }


    // ---Privet-----Helper--------


    // Calculate net Balance effect of journal Line in an Account
    private BigDecimal calculateNetEffect(Account account, List<JournalLine> lines) {
        BigDecimal balance = BigDecimal.ZERO;
        for (JournalLine line : lines) {
            balance = applySingleLineEffect(account, balance, line);
        }
        return balance;
    }


//     Applies Single journal line debit/credit to a running balance (debit=credit),

    private BigDecimal applySingleLineEffect(Account account, BigDecimal currentBalance, JournalLine line) {
        switch (account.getType()) {
            case ASSET:
            case EXPENSE:
                return currentBalance.add(line.getDebit()).subtract(line.getCredit());
            case LIABILITY:
            case EQUITY:
            case REVENUE:
                return currentBalance.add(line.getCredit()).subtract(line.getDebit());
            default:
                return currentBalance;
        }
    }


//    ============================P&L+BalanceSheet Helper================

    private BigDecimal calculatePeriodBalance(
            Account account,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        List<JournalStatus> reportStatuses = List.of(
                JournalStatus.POSTED,
                JournalStatus.REVERSED
        );

        List<JournalLine> lines =
                journalLineRepository
                        .findByAccountIdAndJournalEntry_StatusInAndJournalEntry_DateBetweenOrderByJournalEntry_DateAsc(
                                account.getId(),
                                reportStatuses,
                                fromDate,
                                toDate
                        );

        return calculateNetEffect(account, lines);
    }


//   =============== Aging Helper Methods===============

    private AgingRowDto buildAgingRowFromInvoices(Party party, List<Invoice> invoices, LocalDate asOfDate) {

        BigDecimal current = BigDecimal.ZERO;
        BigDecimal d1to30 = BigDecimal.ZERO;
        BigDecimal d31to60 = BigDecimal.ZERO;
        BigDecimal d61to90 = BigDecimal.ZERO;
        BigDecimal d91plus = BigDecimal.ZERO;

        for (Invoice inv : invoices) {
            long daysOverdue = ChronoUnit.DAYS.between(inv.getDueDate(), asOfDate);
            BigDecimal due = inv.getDueAmount();

            if (daysOverdue <= 0) current = current.add(due);
            else if (daysOverdue <= 30) d1to30 = d1to30.add(due);
            else if (daysOverdue <= 60) d31to60 = d31to60.add(due);
            else if (daysOverdue <= 90) d61to90 = d61to90.add(due);
            else d91plus = d91plus.add(due);
        }

        BigDecimal total = current.add(d1to30).add(d31to60).add(d61to90).add(d91plus);

        return AgingRowDto.builder()
                .partyId(party.getId())
                .partyName(party.getName())
                .current(current)
                .days1to30(d1to30)
                .days31to60(d31to60)
                .days61to90(d61to90)
                .days91Plus(d91plus)
                .totalDue(total)
                .build();
    }

    private AgingRowDto buildAgingRowFromBills(Party party, List<VendorBill> bills, LocalDate asOfDate) {

        BigDecimal current = BigDecimal.ZERO;
        BigDecimal d1to30 = BigDecimal.ZERO;
        BigDecimal d31to60 = BigDecimal.ZERO;
        BigDecimal d61to90 = BigDecimal.ZERO;
        BigDecimal d91plus = BigDecimal.ZERO;

        for (VendorBill bill : bills) {
            long daysOverdue = ChronoUnit.DAYS.between(bill.getDueDate(), asOfDate);
            BigDecimal due = bill.getDueAmount();

            if (daysOverdue <= 0) current = current.add(due);
            else if (daysOverdue <= 30) d1to30 = d1to30.add(due);
            else if (daysOverdue <= 60) d31to60 = d31to60.add(due);
            else if (daysOverdue <= 90) d61to90 = d61to90.add(due);
            else d91plus = d91plus.add(due);
        }

        BigDecimal total = current.add(d1to30).add(d31to60).add(d61to90).add(d91plus);

        return AgingRowDto.builder()
                .partyId(party.getId())
                .partyName(party.getName())
                .current(current)
                .days1to30(d1to30)
                .days31to60(d31to60)
                .days61to90(d61to90)
                .days91Plus(d91plus)
                .totalDue(total)
                .build();
    }

    // =====================Party Opening Balance Helper================

    private BigDecimal calculatePartyBalanceBefore(Long partyId, PartyType partyType, LocalDate fromDate) {

        BigDecimal balance = BigDecimal.ZERO;

        // Invoices before fromDate
        balance = balance.add(invoiceRepository.findByPartyId(partyId).stream()
                .filter(inv -> inv.getStatus() != InvoiceStatus.CANCELLED)
                .filter(inv -> inv.getInvoiceDate().isBefore(fromDate))
                .map(Invoice::getGrandTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        // Vendor bills before fromDate
        balance = balance.subtract(vendorBillRepository.findByPartyId(partyId).stream()
                .filter(bill -> bill.getStatus() != VendorBillStatus.CANCELLED)
                .filter(bill -> bill.getBillDate().isBefore(fromDate))
                .map(VendorBill::getNetPayable)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        // Payments before fromDate
        for (Payment p : paymentRepository.findByPartyId(partyId)) {
            if (p.getStatus() != com.nexaerp.payment.PaymentStatus.POSTED) continue;
            if (!p.getPaymentDate().isBefore(fromDate)) continue;

            if (p.getPaymentType() == PaymentType.RECEIPT) {
                balance = balance.subtract(p.getAmount());
            } else {
                balance = balance.add(p.getAmount());
            }
        }

        return balance;

    }

    private boolean isLeafAccount(Account account) {
        return account.getChildren() == null
                || account.getChildren().isEmpty();
    }
}
