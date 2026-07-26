package com.nexaerp.report;

import com.nexaerp.account.*;
import com.nexaerp.banking.repository.BankAccountRepository;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.journal.*;
import com.nexaerp.report.dto.CashFlowStatementResponseDto;
import com.nexaerp.settings.SystemSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CashFlowStatementServiceImplTest {
    @Mock AccountRepository accountRepository;
    @Mock BankAccountRepository bankAccountRepository;
    @Mock JournalLineRepository journalLineRepository;
    @Mock SystemSettingsService systemSettingsService;
    CashFlowStatementServiceImpl service;
    Account cash;
    long journalSequence;

    @Test
    void expectedBusinessRuleFailureDoesNotPoisonAJoiningReadTransaction() {
        Transactional transaction = CashFlowStatementServiceImpl.class.getAnnotation(Transactional.class);

        assertThat(transaction).isNotNull();
        assertThat(transaction.readOnly()).isTrue();
        assertThat(transaction.noRollbackFor()).contains(BusinessRuleException.class);
    }

    @BeforeEach void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new CashFlowStatementServiceImpl(accountRepository, bankAccountRepository,
                journalLineRepository, systemSettingsService);
        cash = account(1L, "1100", "Cash", AccountType.ASSET, true);
        when(accountRepository.findByIsCashEquivalentTrue()).thenReturn(List.of(cash));
        when(bankAccountRepository.findActiveLinkedCoaAccountIds()).thenReturn(List.of());
        when(accountRepository.findAllById(any())).thenReturn(List.of(cash));
        when(bankAccountRepository.findByIsActive(true)).thenReturn(List.of());
        when(systemSettingsService.getValue(any())).thenThrow(new RuntimeException("not configured"));
        when(systemSettingsService.getAccountId(any())).thenThrow(new RuntimeException("not configured"));
        when(journalLineRepository.sumCashEffectBefore(anyList(), anyList(), any())).thenReturn(BigDecimal.ZERO);
        when(journalLineRepository.sumCashEffectThrough(anyList(), anyList(), any())).thenReturn(BigDecimal.ZERO);
        when(journalLineRepository.aggregateCashAccountBalances(anyList(), anyList(), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{1L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO}));
        when(journalLineRepository.findJournalIdsContainingCash(anyList(), anyList(), any(), any())).thenReturn(List.of());
    }

    @Test void customerReceiptIsOperatingInflow() {
        Account receivable = account(2L, "1200", "Receivable", AccountType.ASSET, false);
        doReturn(2L).when(systemSettingsService).getAccountId(com.nexaerp.settings.SettingKey.DEFAULT_RECEIVABLE_ACCOUNT);
        CashFlowStatementResponseDto result = report(journal(JournalSourceType.PAYMENT, JournalEntryType.CASH,
                line(cash, "100", "0"), line(receivable, "0", "100")), "100");
        assertThat(result.getOperatingActivities().getItems()).extracting(i -> i.getLineItem())
                .contains(CashFlowLineItem.CUSTOMER_RECEIPTS);
        assertThat(result.getNetCashFromOperatingActivities()).isEqualByComparingTo("100.00");
    }

    @Test void vendorPaymentIsOperatingOutflow() {
        Account payable = account(2L, "2100", "Payable", AccountType.LIABILITY, false);
        doReturn(2L).when(systemSettingsService).getAccountId(com.nexaerp.settings.SettingKey.DEFAULT_PAYABLE_ACCOUNT);
        CashFlowStatementResponseDto result = report(journal(JournalSourceType.PAYMENT, JournalEntryType.CASH,
                line(payable, "75", "0"), line(cash, "0", "75")), "-75");
        assertThat(result.getOperatingActivities().getItems()).extracting(i -> i.getLineItem())
                .contains(CashFlowLineItem.SUPPLIER_PAYMENTS);
    }

    @Test void immediateExpenseAndInterestExpenseAreOperatingOutflows() {
        Account expense = account(2L, "5100", "Finance cost", AccountType.EXPENSE, false);
        CashFlowStatementResponseDto result = report(journal(JournalSourceType.EXPENSE_CLAIM, JournalEntryType.CASH,
                line(expense, "40", "0"), line(cash, "0", "40")), "-40");
        assertThat(result.getNetCashFromOperatingActivities()).isEqualByComparingTo("-40.00");
    }

    @ParameterizedTest
    @EnumSource(value = JournalSourceType.class, names = {"VENDOR_BILL", "FIXED_ASSET"})
    void creditExpenseAssetAndDepreciationWithoutCashDoNotAppear(JournalSourceType source) {
        when(journalLineRepository.findJournalIdsContainingCash(anyList(), anyList(), any(), any())).thenReturn(List.of());
        CashFlowStatementResponseDto result = service.generate(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));
        assertThat(result.getNetChangeInCash()).isEqualByComparingTo("0.00");
    }

    @Test void fixedAssetCashPurchaseIsInvestingOutflow() {
        Account asset = account(2L, "1500", "Equipment", AccountType.ASSET, false);
        CashFlowStatementResponseDto result = report(journal(JournalSourceType.FIXED_ASSET, JournalEntryType.ASSET,
                line(asset, "300", "0"), line(cash, "0", "300")), "-300");
        assertThat(result.getInvestingActivities().getItems()).extracting(i -> i.getLineItem())
                .contains(CashFlowLineItem.FIXED_ASSET_PURCHASE);
    }

    @Test void fixedAssetDisposalUsesNetCashAndDoesNotClassifyGainLossSeparately() {
        Account asset = account(2L, "1500", "Equipment", AccountType.ASSET, false);
        Account accumulated = account(3L, "1590", "Accumulated depreciation", AccountType.ASSET, false);
        Account gain = account(4L, "4200", "Disposal gain", AccountType.REVENUE, false);
        CashFlowStatementResponseDto result = report(journal(JournalSourceType.FIXED_ASSET, JournalEntryType.ASSET,
                line(cash, "50", "0"), line(accumulated, "60", "0"), line(asset, "0", "100"), line(gain, "0", "10")), "50");
        assertThat(result.getInvestingActivities().getItems()).singleElement()
                .satisfies(item -> {
                    assertThat(item.getLineItem()).isEqualTo(CashFlowLineItem.FIXED_ASSET_DISPOSAL);
                    assertThat(item.getInflow()).isEqualByComparingTo("50.00");
                });
        assertThat(result.getNetCashFromOperatingActivities()).isEqualByComparingTo("0.00");
    }

    @Test void genericLiabilityReceiptIsNotAutomaticallyLoanProceeds() {
        Account liability = account(2L, "2200", "Current liability", AccountType.LIABILITY, false);
        CashFlowStatementResponseDto result = report(journal(JournalSourceType.BANK_TRANSACTION, JournalEntryType.BANK,
                line(cash, "100", "0"), line(liability, "0", "100")), "100");
        assertThat(result.getFinancingActivities().getItems()).isEmpty();
        assertThat(result.getUnclassifiedMovements()).singleElement()
                .satisfies(warning -> assertThat(warning.getReason()).contains("not a configured payable, tax"));
    }

    @Test void genericLiabilityPaymentIsNotAutomaticallyLoanRepayment() {
        Account liability = account(2L, "2200", "Current liability", AccountType.LIABILITY, false);
        CashFlowStatementResponseDto result = report(journal(JournalSourceType.BANK_TRANSACTION, JournalEntryType.BANK,
                line(liability, "70", "0"), line(cash, "0", "70")), "-70");
        assertThat(result.getFinancingActivities().getItems()).isEmpty();
        assertThat(result.getUnclassifiedMovements()).hasSize(1);
        assertThat(result.getIsReconciled()).isFalse();
    }

    @Test void ordinaryExpenseNamedInterestDoesNotEmitInterestPaid() {
        Account expense = account(2L, "5200", "Interest expense", AccountType.EXPENSE, false);
        CashFlowStatementResponseDto result = report(journal(JournalSourceType.BANK_TRANSACTION, JournalEntryType.BANK,
                line(expense, "20", "0"), line(cash, "0", "20")), "-20");
        assertThat(result.getOperatingActivities().getItems()).extracting(i -> i.getLineItem())
                .contains(CashFlowLineItem.OPERATING_EXPENSES)
                .doesNotContain(CashFlowLineItem.INTEREST_PAID);
    }

    @Test void ordinaryRevenueNamedInterestDoesNotEmitInterestReceived() {
        Account revenue = account(2L, "4200", "Interest income", AccountType.REVENUE, false);
        CashFlowStatementResponseDto result = report(journal(JournalSourceType.BANK_TRANSACTION, JournalEntryType.BANK,
                line(cash, "20", "0"), line(revenue, "0", "20")), "20");
        assertThat(result.getOperatingActivities().getItems()).extracting(i -> i.getLineItem())
                .contains(CashFlowLineItem.OTHER_OPERATING)
                .doesNotContain(CashFlowLineItem.INTEREST_RECEIVED);
    }

    @Test void transfersBetweenCashAccountsAreExcluded() {
        Account bank = account(2L, "1110", "Bank", AccountType.ASSET, true);
        when(accountRepository.findByIsCashEquivalentTrue()).thenReturn(List.of(cash, bank));
        when(accountRepository.findAllById(any())).thenReturn(List.of(cash, bank));
        JournalEntry journal = journal(JournalSourceType.BANK_TRANSACTION, JournalEntryType.BANK,
                line(bank, "100", "0"), line(cash, "0", "100"));
        CashFlowStatementResponseDto result = reportWithAccounts(journal, "0", List.of(cash, bank));
        assertThat(result.getNetChangeInCash()).isEqualByComparingTo("0.00");
        assertThat(result.getUnclassifiedMovements()).isEmpty();
    }

    @Test void manualUnknownAssetMovementProducesWarningAndDifference() {
        Account asset = account(2L, "1900", "Other asset", AccountType.ASSET, false);
        CashFlowStatementResponseDto result = report(journal(JournalSourceType.MANUAL, JournalEntryType.GENERAL,
                line(cash, "25", "0"), line(asset, "0", "25")), "25");
        assertThat(result.getUnclassifiedMovements()).hasSize(1);
        assertThat(result.getIsReconciled()).isFalse();
    }

    @Test void openingIsStrictlyBeforeAndClosingIsThroughEndDate() {
        when(journalLineRepository.sumCashEffectBefore(anyList(), anyList(), eq(LocalDate.of(2026, 7, 1))))
                .thenReturn(new BigDecimal("90"));
        when(journalLineRepository.sumCashEffectThrough(anyList(), anyList(), eq(LocalDate.of(2026, 7, 31))))
                .thenReturn(new BigDecimal("90"));
        CashFlowStatementResponseDto result = service.generate(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));
        assertThat(result.getOpeningCashBalance()).isEqualByComparingTo("90.00");
        assertThat(result.getLedgerClosingCashBalance()).isEqualByComparingTo("90.00");
    }

    @Test void originalReversalAndSamePeriodReversalNetToZero() {
        Account revenue = account(2L, "4100", "Revenue", AccountType.REVENUE, false);
        JournalEntry original = journal(JournalSourceType.PAYMENT, JournalEntryType.CASH, line(cash, "60", "0"), line(revenue, "0", "60"));
        original.setStatus(JournalStatus.REVERSED);
        JournalEntry reversal = journal(JournalSourceType.PAYMENT, JournalEntryType.CASH, line(cash, "0", "60"), line(revenue, "60", "0"));
        reversal.setReversedFromId(original.getId());
        CashFlowStatementResponseDto result = report(List.of(original, reversal), "0");
        assertThat(result.getNetChangeInCash()).isEqualByComparingTo("0.00");
        assertThat(result.getIsReconciled()).isTrue();
    }

    @Test void crossPeriodReversalUsesAccountingDate() {
        Account revenue = account(2L, "4100", "Revenue", AccountType.REVENUE, false);
        JournalEntry reversal = journal(JournalSourceType.PAYMENT, JournalEntryType.CASH, line(cash, "0", "60"), line(revenue, "60", "0"));
        reversal.setDate(LocalDate.of(2026, 7, 10));
        CashFlowStatementResponseDto result = report(reversal, "-60");
        assertThat(result.getNetCashFromOperatingActivities()).isEqualByComparingTo("-60.00");
    }

    @Test void invalidRangeAndMissingCashAccountsAreRejected() {
        assertThatThrownBy(() -> service.generate(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 7, 1)))
                .isInstanceOf(BusinessRuleException.class);
        when(accountRepository.findByIsCashEquivalentTrue()).thenReturn(List.of());
        assertThatThrownBy(() -> service.generate(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("No cash");
    }

    private CashFlowStatementResponseDto report(JournalEntry journal, String closing) { return report(List.of(journal), closing); }
    private CashFlowStatementResponseDto report(List<JournalEntry> journals, String closing) {
        return reportWithAccounts(journals, closing, List.of(cash));
    }
    private CashFlowStatementResponseDto reportWithAccounts(JournalEntry journal, String closing, List<Account> accounts) {
        return reportWithAccounts(List.of(journal), closing, accounts);
    }
    private CashFlowStatementResponseDto reportWithAccounts(List<JournalEntry> journals, String closing, List<Account> accounts) {
        List<Long> ids = journals.stream().map(JournalEntry::getId).toList();
        List<JournalLine> lines = journals.stream().flatMap(j -> j.getLines().stream()).toList();
        when(journalLineRepository.findJournalIdsContainingCash(anyList(), anyList(), any(), any())).thenReturn(ids);
        when(journalLineRepository.findAllForJournalIds(ids)).thenReturn(lines);
        when(journalLineRepository.sumCashEffectThrough(anyList(), anyList(), any())).thenReturn(new BigDecimal(closing));
        List<Object[]> aggregates = accounts.stream().map(a -> new Object[]{a.getId(), BigDecimal.ZERO,
                a.getId().equals(cash.getId()) ? new BigDecimal(closing) : BigDecimal.ZERO,
                a.getId().equals(cash.getId()) ? new BigDecimal(closing) : BigDecimal.ZERO}).toList();
        when(journalLineRepository.aggregateCashAccountBalances(anyList(), anyList(), any(), any())).thenReturn(aggregates);
        return service.generate(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));
    }

    private JournalEntry journal(JournalSourceType source, JournalEntryType type, JournalLine... lines) {
        JournalEntry journal = new JournalEntry(); journal.setId(++journalSequence); journal.setEntryNumber("JE-" + journalSequence);
        journal.setDate(LocalDate.of(2026, 7, 5)); journal.setStatus(JournalStatus.POSTED); journal.setSourceType(source); journal.setType(type); journal.setLines(List.of(lines));
        for (JournalLine line : lines) line.setJournalEntry(journal);
        return journal;
    }
    private JournalLine line(Account account, String debit, String credit) {
        JournalLine line = new JournalLine(); line.setAccount(account); line.setDebit(new BigDecimal(debit)); line.setCredit(new BigDecimal(credit)); return line;
    }
    private Account account(Long id, String code, String name, AccountType type, boolean cashEquivalent) {
        Account account = new Account(); account.setId(id); account.setCode(code); account.setName(name); account.setType(type); account.setIsActive(true); account.setIsCashEquivalent(cashEquivalent); return account;
    }
}
