package com.nexaerp.report;

import com.nexaerp.account.*;
import com.nexaerp.banking.entity.BankAccount;
import com.nexaerp.banking.repository.BankAccountRepository;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.journal.*;
import com.nexaerp.report.dto.*;
import com.nexaerp.settings.SettingKey;
import com.nexaerp.settings.SystemSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, noRollbackFor = BusinessRuleException.class)
public class CashFlowStatementServiceImpl implements CashFlowStatementService {
    private static final int MONEY_SCALE = 2;
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(MONEY_SCALE);
    private static final List<JournalStatus> REPORT_STATUSES =
            List.of(JournalStatus.POSTED, JournalStatus.REVERSED);

    private final AccountRepository accountRepository;
    private final BankAccountRepository bankAccountRepository;
    private final JournalLineRepository journalLineRepository;
    private final SystemSettingsService systemSettingsService;

    @Override
    public CashFlowStatementResponseDto generate(LocalDate fromDate, LocalDate toDate) {
        validateDates(fromDate, toDate);

        Map<Long, Account> cashAccounts = resolveCashAccounts();
        List<Long> cashAccountIds = new ArrayList<>(cashAccounts.keySet());
        String currency = resolveBaseCurrency();
        validateCurrencies(cashAccountIds, currency);

        BigDecimal opening = money(journalLineRepository.sumCashEffectBefore(
                cashAccountIds, REPORT_STATUSES, fromDate));
        BigDecimal ledgerClosing = money(journalLineRepository.sumCashEffectThrough(
                cashAccountIds, REPORT_STATUSES, toDate));

        Map<CashFlowActivity, ActivityAccumulator> activities = new EnumMap<>(CashFlowActivity.class);
        activities.put(CashFlowActivity.OPERATING, new ActivityAccumulator());
        activities.put(CashFlowActivity.INVESTING, new ActivityAccumulator());
        activities.put(CashFlowActivity.FINANCING, new ActivityAccumulator());
        List<UnclassifiedCashMovementDto> warnings = new ArrayList<>();

        List<Long> journalIds = journalLineRepository.findJournalIdsContainingCash(
                cashAccountIds, REPORT_STATUSES, fromDate, toDate);
        if (!journalIds.isEmpty()) {
            Map<Long, List<JournalLine>> linesByJournal = journalLineRepository
                    .findAllForJournalIds(journalIds).stream()
                    .collect(Collectors.groupingBy(line -> line.getJournalEntry().getId(), LinkedHashMap::new,
                            Collectors.toList()));
            ClassificationContext context = loadClassificationContext();
            for (List<JournalLine> lines : linesByJournal.values()) {
                classifyJournal(lines, cashAccounts.keySet(), context, activities, warnings);
            }
        }

        CashFlowActivitySectionDto operating = section(CashFlowActivity.OPERATING, activities.get(CashFlowActivity.OPERATING));
        CashFlowActivitySectionDto investing = section(CashFlowActivity.INVESTING, activities.get(CashFlowActivity.INVESTING));
        CashFlowActivitySectionDto financing = section(CashFlowActivity.FINANCING, activities.get(CashFlowActivity.FINANCING));
        BigDecimal netChange = money(operating.getNetCashFlow().add(investing.getNetCashFlow())
                .add(financing.getNetCashFlow()));
        BigDecimal calculatedClosing = money(opening.add(netChange));
        BigDecimal difference = money(calculatedClosing.subtract(ledgerClosing));

        return CashFlowStatementResponseDto.builder()
                .fromDate(fromDate).toDate(toDate).currencyCode(currency)
                .openingCashBalance(opening)
                .operatingActivities(operating).netCashFromOperatingActivities(operating.getNetCashFlow())
                .investingActivities(investing).netCashFromInvestingActivities(investing.getNetCashFlow())
                .financingActivities(financing).netCashFromFinancingActivities(financing.getNetCashFlow())
                .netChangeInCash(netChange)
                .calculatedClosingCashBalance(calculatedClosing)
                .ledgerClosingCashBalance(ledgerClosing)
                .reconciliationDifference(difference)
                .isReconciled(difference.compareTo(ZERO) == 0)
                .cashAccounts(buildAccountBreakdown(cashAccounts, cashAccountIds, fromDate, toDate))
                .unclassifiedMovements(warnings)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private void validateDates(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) throw new BusinessRuleException("From date and to date are required");
        if (fromDate.isAfter(toDate)) throw new BusinessRuleException("From date cannot be after to date");
    }

    private Map<Long, Account> resolveCashAccounts() {
        Set<Long> ids = accountRepository.findByIsCashEquivalentTrue().stream()
                .map(Account::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        ids.addAll(bankAccountRepository.findActiveLinkedCoaAccountIds());
        if (ids.isEmpty()) throw new BusinessRuleException("No cash or cash-equivalent accounts are configured");
        Map<Long, Account> accounts = accountRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Account::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        if (accounts.size() != ids.size()) throw new BusinessRuleException("A bank account references a missing Chart of Accounts account");
        for (Account account : accounts.values()) {
            if (account.getType() != AccountType.ASSET) {
                throw new BusinessRuleException("Cash account " + account.getCode() + " must be an ASSET account");
            }
        }
        return accounts;
    }

    private String resolveBaseCurrency() {
        try {
            String value = systemSettingsService.getValue(SettingKey.DEFAULT_CURRENCY);
            return value == null || value.isBlank() ? "BDT" : value.trim().toUpperCase(Locale.ROOT);
        } catch (RuntimeException ignored) {
            return "BDT";
        }
    }

    private void validateCurrencies(List<Long> cashIds, String currency) {
        for (BankAccount bank : bankAccountRepository.findByIsActive(true)) {
            if (bank.getCoaAccountId() != null && cashIds.contains(bank.getCoaAccountId())
                    && bank.getCurrency() != null && !currency.equalsIgnoreCase(bank.getCurrency())) {
                throw new BusinessRuleException("Cash Flow supports base currency " + currency
                        + " only; account " + bank.getAccountName() + " uses " + bank.getCurrency());
            }
        }
    }

    private ClassificationContext loadClassificationContext() {
        return new ClassificationContext(optionalAccountId(SettingKey.DEFAULT_RECEIVABLE_ACCOUNT),
                optionalAccountId(SettingKey.DEFAULT_PAYABLE_ACCOUNT),
                optionalAccountId(SettingKey.DEFAULT_VAT_PAYABLE),
                optionalAccountId(SettingKey.DEFAULT_INPUT_VAT),
                optionalAccountId(SettingKey.DEFAULT_TDS_PAYABLE));
    }

    private Long optionalAccountId(SettingKey key) {
        try { return systemSettingsService.getAccountId(key); }
        catch (RuntimeException ignored) { return null; }
    }

    private void classifyJournal(List<JournalLine> lines, Set<Long> cashIds, ClassificationContext context,
                                 Map<CashFlowActivity, ActivityAccumulator> activities,
                                 List<UnclassifiedCashMovementDto> warnings) {
        if (lines.isEmpty()) return;
        JournalEntry journal = lines.get(0).getJournalEntry();
        List<JournalLine> nonCashLines = lines.stream().filter(l -> !cashIds.contains(l.getAccount().getId())).toList();
        BigDecimal cashMovement = money(lines.stream().filter(l -> cashIds.contains(l.getAccount().getId()))
                .map(l -> amount(l.getDebit()).subtract(amount(l.getCredit())))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        if (cashMovement.compareTo(ZERO) == 0 && nonCashLines.isEmpty()) return;
        if (nonCashLines.isEmpty()) return;

        if (journal.getSourceType() == JournalSourceType.FIXED_ASSET) {
            if (cashMovement.compareTo(ZERO) > 0) {
                activities.get(CashFlowActivity.INVESTING)
                        .add(CashFlowLineItem.FIXED_ASSET_DISPOSAL, cashMovement, ZERO);
            } else if (cashMovement.compareTo(ZERO) < 0) {
                activities.get(CashFlowActivity.INVESTING)
                        .add(CashFlowLineItem.FIXED_ASSET_PURCHASE, ZERO, cashMovement.abs());
            }
            return;
        }

        BigDecimal classifiedNet = ZERO;
        for (JournalLine line : nonCashLines) {
            BigDecimal inflow = money(amount(line.getCredit()));
            BigDecimal outflow = money(amount(line.getDebit()));
            if (inflow.compareTo(ZERO) == 0 && outflow.compareTo(ZERO) == 0) continue;
            Classification classification = classify(journal, line, context, inflow, outflow);
            BigDecimal net = money(inflow.subtract(outflow));
            if (classification.activity == CashFlowActivity.UNCLASSIFIED) {
                warnings.add(warning(journal, net, classification.reason));
                continue;
            }
            activities.get(classification.activity).add(classification.lineItem, inflow, outflow);
            classifiedNet = money(classifiedNet.add(net));
        }
        BigDecimal unresolved = money(cashMovement.subtract(classifiedNet));
        if (unresolved.compareTo(ZERO) != 0 && warnings.stream().noneMatch(w -> w.getJournalEntryId().equals(journal.getId()))) {
            warnings.add(warning(journal, unresolved, "Cash movement could not be deterministically allocated"));
        }
    }

    private Classification classify(JournalEntry journal, JournalLine line, ClassificationContext c,
                                    BigDecimal inflow, BigDecimal outflow) {
        Account account = line.getAccount();
        boolean moneyIn = inflow.compareTo(ZERO) > 0;
        if (Objects.equals(account.getId(), c.receivableId))
            return result(CashFlowActivity.OPERATING, CashFlowLineItem.CUSTOMER_RECEIPTS);
        if (Objects.equals(account.getId(), c.payableId))
            return result(CashFlowActivity.OPERATING, CashFlowLineItem.SUPPLIER_PAYMENTS);
        if (Objects.equals(account.getId(), c.vatPayableId) || Objects.equals(account.getId(), c.inputVatId)
                || Objects.equals(account.getId(), c.tdsPayableId))
            return result(CashFlowActivity.OPERATING, CashFlowLineItem.TAX_PAYMENTS);
        if (journal.getSourceType() == JournalSourceType.FIXED_ASSET && account.getType() == AccountType.ASSET)
            return result(CashFlowActivity.INVESTING,
                    moneyIn ? CashFlowLineItem.FIXED_ASSET_DISPOSAL : CashFlowLineItem.FIXED_ASSET_PURCHASE);
        if (account.getType() == AccountType.EXPENSE)
            return result(CashFlowActivity.OPERATING, CashFlowLineItem.OPERATING_EXPENSES);
        if (account.getType() == AccountType.REVENUE)
            return result(CashFlowActivity.OPERATING, CashFlowLineItem.OTHER_OPERATING);
        if (account.getType() == AccountType.EQUITY)
            return result(CashFlowActivity.FINANCING,
                    moneyIn ? CashFlowLineItem.CAPITAL_INTRODUCED : CashFlowLineItem.OWNER_WITHDRAWALS);
        if (account.getType() == AccountType.LIABILITY)
            return new Classification(CashFlowActivity.UNCLASSIFIED, CashFlowLineItem.UNCLASSIFIED,
                    "Liability account " + account.getCode()
                            + " is not a configured payable, tax, or reliably identified loan account");
        if (account.getType() == AccountType.ASSET && journal.getSourceType() != JournalSourceType.MANUAL)
            return result(CashFlowActivity.INVESTING, CashFlowLineItem.OTHER_INVESTING);
        return new Classification(CashFlowActivity.UNCLASSIFIED, CashFlowLineItem.UNCLASSIFIED,
                "No reliable classification exists for counterpart account " + account.getCode());
    }

    private Classification result(CashFlowActivity activity, CashFlowLineItem item) {
        return new Classification(activity, item, null);
    }

    private UnclassifiedCashMovementDto warning(JournalEntry journal, BigDecimal amount, String reason) {
        return UnclassifiedCashMovementDto.builder().journalEntryId(journal.getId())
                .entryNumber(journal.getEntryNumber()).date(journal.getDate()).sourceType(journal.getSourceType())
                .sourceId(journal.getSourceId()).description(journal.getDescription()).amount(money(amount)).reason(reason).build();
    }

    private CashFlowActivitySectionDto section(CashFlowActivity activity, ActivityAccumulator accumulator) {
        List<CashFlowLineItemDto> items = accumulator.values.entrySet().stream()
                .map(e -> CashFlowLineItemDto.builder().lineItem(e.getKey()).label(label(e.getKey()))
                        .inflow(money(e.getValue().inflow)).outflow(money(e.getValue().outflow))
                        .netAmount(money(e.getValue().inflow.subtract(e.getValue().outflow))).build())
                .toList();
        BigDecimal inflows = money(items.stream().map(CashFlowLineItemDto::getInflow).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal outflows = money(items.stream().map(CashFlowLineItemDto::getOutflow).reduce(BigDecimal.ZERO, BigDecimal::add));
        return CashFlowActivitySectionDto.builder().activity(activity).items(items).totalInflows(inflows)
                .totalOutflows(outflows).netCashFlow(money(inflows.subtract(outflows))).build();
    }

    private List<CashFlowAccountBalanceDto> buildAccountBreakdown(Map<Long, Account> accounts, List<Long> ids,
                                                                  LocalDate fromDate, LocalDate toDate) {
        Map<Long, Object[]> aggregates = journalLineRepository.aggregateCashAccountBalances(ids, REPORT_STATUSES, fromDate, toDate)
                .stream().collect(Collectors.toMap(row -> ((Number) row[0]).longValue(), Function.identity()));
        return accounts.values().stream().sorted(Comparator.comparing(Account::getCode)).map(account -> {
            Object[] row = aggregates.get(account.getId());
            return CashFlowAccountBalanceDto.builder().accountId(account.getId()).accountCode(account.getCode())
                    .accountName(account.getName()).openingBalance(row == null ? ZERO : money((BigDecimal) row[1]))
                    .periodMovement(row == null ? ZERO : money((BigDecimal) row[2]))
                    .closingBalance(row == null ? ZERO : money((BigDecimal) row[3])).build();
        }).toList();
    }

    private String label(CashFlowLineItem item) {
        String text = item.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private BigDecimal amount(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private BigDecimal money(BigDecimal value) { return amount(value).setScale(MONEY_SCALE, RoundingMode.HALF_UP); }

    private record ClassificationContext(Long receivableId, Long payableId, Long vatPayableId,
                                         Long inputVatId, Long tdsPayableId) {}
    private record Classification(CashFlowActivity activity, CashFlowLineItem lineItem, String reason) {}
    private static class Totals { BigDecimal inflow = ZERO; BigDecimal outflow = ZERO; }
    private static class ActivityAccumulator {
        private final Map<CashFlowLineItem, Totals> values = new EnumMap<>(CashFlowLineItem.class);
        void add(CashFlowLineItem item, BigDecimal inflow, BigDecimal outflow) {
            Totals totals = values.computeIfAbsent(item, ignored -> new Totals());
            totals.inflow = totals.inflow.add(inflow);
            totals.outflow = totals.outflow.add(outflow);
        }
    }
}
