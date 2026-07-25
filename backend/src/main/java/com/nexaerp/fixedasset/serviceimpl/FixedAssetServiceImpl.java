package com.nexaerp.fixedasset.serviceimpl;

import com.nexaerp.account.Account;
import com.nexaerp.account.AccountRepository;
import com.nexaerp.account.AccountType;
import com.nexaerp.accountingperiod.AccountingPeriodService;
import com.nexaerp.audit.AuditAction;
import com.nexaerp.audit.AuditLogService;
import com.nexaerp.banking.services.BankTransactionService;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.fixedasset.*;
import com.nexaerp.fixedasset.dto.*;
import com.nexaerp.fixedasset.repository.DepreciationEntryRepository;
import com.nexaerp.fixedasset.repository.FixedAssetRepository;
import com.nexaerp.fixedasset.services.FixedAssetService;
import com.nexaerp.journal.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FixedAssetServiceImpl implements FixedAssetService {

    private final FixedAssetRepository fixedAssetRepository;
    private final DepreciationEntryRepository depreciationEntryRepository;
    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalLineRepository journalLineRepository;
    private final AccountingPeriodService accountingPeriodService;
    private final BankTransactionService bankTransactionService;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public FixedAssetResponseDto create(FixedAssetRequestDto request) {
        Account assetAccount = getAccount(request.getAssetAccountId());
        Account depreciationExpenseAccount = getAccount(request.getDepreciationExpenseAccountId());
        Account accumulatedDepreciationAccount = getAccount(request.getAccumulatedDepreciationAccountId());
        Account paymentSourceAccount = getAccount(request.getPaymentSourceAccountId());

        if (assetAccount.getType() != AccountType.ASSET) {
            throw new BusinessRuleException("Asset account must be of type ASSET");
        }
        if (accumulatedDepreciationAccount.getType() != AccountType.ASSET) {
            throw new BusinessRuleException("Accumulated depreciation account must be of type ASSET (contra-asset)");
        }
        if (depreciationExpenseAccount.getType() != AccountType.EXPENSE) {
            throw new BusinessRuleException("Depreciation expense account must be of type EXPENSE");
        }
        if (paymentSourceAccount.getId().equals(assetAccount.getId())) {
            throw new BusinessRuleException("Payment source account cannot be the same as the asset account");
        }
        if (request.getSalvageValue().compareTo(request.getPurchaseCost()) >= 0) {
            throw new BusinessRuleException("Salvage value must be less than purchase cost");
        }
        if (request.getDepreciationMethod() == DepreciationMethod.REDUCING_BALANCE
                && (request.getReducingBalanceRate() == null
                || request.getReducingBalanceRate().compareTo(BigDecimal.ZERO) <= 0)) {
            throw new BusinessRuleException("Reducing balance rate is required and must be greater than 0");
        }

        accountingPeriodService.validatePostingDate(request.getPurchaseDate());

        FixedAsset asset = FixedAsset.builder()
                .assetCode(generateAssetCode())
                .name(request.getName())
                .description(request.getDescription())
                .assetAccount(assetAccount)
                .depreciationExpenseAccount(depreciationExpenseAccount)
                .accumulatedDepreciationAccount(accumulatedDepreciationAccount)
                .purchaseDate(request.getPurchaseDate())
                .purchaseCost(request.getPurchaseCost())
                .salvageValue(request.getSalvageValue())
                .usefulLifeYears(request.getUsefulLifeYears())
                .depreciationMethod(request.getDepreciationMethod())
                .reducingBalanceRate(request.getReducingBalanceRate())
                .accumulatedDepreciation(BigDecimal.ZERO)
                .status(AssetStatus.ACTIVE)
                .build();

        FixedAsset saved = fixedAssetRepository.save(asset);

        // Purchase entry: Dr Asset, Cr payment source (Cash/Bank/Accounts Payable).
        // addLine() below automatically mirrors into the Banking module if the payment
        // source COA account happens to be linked to a BankAccount — see mirrorFromJournal().
        JournalEntry entry = new JournalEntry();
        entry.setEntryNumber(generateJournalNumber());
        entry.setDate(request.getPurchaseDate());
        entry.setDescription("Asset purchase - " + saved.getAssetCode() + " - " + saved.getName());
        entry.setType(JournalEntryType.ASSET);
        entry.setStatus(JournalStatus.POSTED);
        entry.setSourceType(JournalSourceType.FIXED_ASSET);
        entry.setSourceId(saved.getId());
        entry.setTotalAmount(request.getPurchaseCost());
        JournalEntry savedEntry = journalEntryRepository.save(entry);

        addLine(savedEntry, assetAccount, request.getPurchaseCost(), BigDecimal.ZERO,
                "Asset purchase - " + saved.getAssetCode());
        addLine(savedEntry, paymentSourceAccount, BigDecimal.ZERO, request.getPurchaseCost(),
                "Asset purchase - " + saved.getAssetCode());

        auditLogService.log(
                AuditAction.CREATED,
                "FIXED_ASSET",
                saved.getId(),
                null,
                saved.getAssetCode() + " - " + saved.getName()
        );

        return toResponse(saved);
    }

    @Override
    public FixedAssetResponseDto getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    public List<FixedAssetResponseDto> getAll() {
        return fixedAssetRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<DepreciationEntryResponseDto> getDepreciationHistory(Long fixedAssetId) {
        findOrThrow(fixedAssetId);
        return depreciationEntryRepository.findByFixedAssetIdOrderByPeriodDateDesc(fixedAssetId).stream()
                .map(this::toDepreciationResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DepreciationEntryResponseDto runDepreciation(Long fixedAssetId, LocalDate asOfDate) {
        FixedAsset asset = findOrThrow(fixedAssetId);

        if (asset.getStatus() != AssetStatus.ACTIVE) {
            throw new BusinessRuleException(
                    "Asset " + asset.getAssetCode() + " is " + asset.getStatus() + " — depreciation cannot be run");
        }

        Optional<DepreciationEntry> result = runDepreciationInternal(asset, asOfDate);

        return result.map(this::toDepreciationResponse)
                .orElseThrow(() -> new BusinessRuleException(
                        "No depreciation due for " + asset.getAssetCode() + " as of " + asOfDate));
    }

    @Override
    @Transactional
    public List<DepreciationEntryResponseDto> runDepreciationForAll(LocalDate asOfDate) {
        List<FixedAsset> activeAssets = fixedAssetRepository.findByStatus(AssetStatus.ACTIVE);

        return activeAssets.stream()
                .map(asset -> runDepreciationInternal(asset, asOfDate))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::toDepreciationResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FixedAssetResponseDto dispose(Long fixedAssetId, AssetDisposalRequestDto request) {
        FixedAsset asset = findOrThrow(fixedAssetId);

        if (asset.getStatus() == AssetStatus.DISPOSED) {
            throw new BusinessRuleException("Asset " + asset.getAssetCode() + " is already disposed");
        }

        accountingPeriodService.validatePostingDate(request.getDisposalDate());

        // Catch up depreciation to the disposal date first, so book value is accurate
        if (asset.getStatus() == AssetStatus.ACTIVE) {
            runDepreciationInternal(asset, request.getDisposalDate());
        }

        BigDecimal bookValue = asset.bookValue();
        BigDecimal proceeds = request.getDisposalProceeds();
        BigDecimal gainLoss = proceeds.subtract(bookValue);

        if (gainLoss.compareTo(BigDecimal.ZERO) != 0 && request.getGainLossAccountId() == null) {
            throw new BusinessRuleException(
                    "There is a gain/loss of " + gainLoss + " on disposal — gainLossAccountId is required");
        }
        if (proceeds.compareTo(BigDecimal.ZERO) > 0 && request.getProceedsAccountId() == null) {
            throw new BusinessRuleException("proceedsAccountId is required when disposal proceeds > 0");
        }

        JournalEntry entry = new JournalEntry();
        entry.setEntryNumber(generateJournalNumber());
        entry.setDate(request.getDisposalDate());
        entry.setDescription("Disposal - " + asset.getAssetCode() + " - " + asset.getName());
        entry.setType(JournalEntryType.ASSET);
        entry.setStatus(JournalStatus.POSTED);
        entry.setSourceType(JournalSourceType.FIXED_ASSET);
        entry.setSourceId(asset.getId());
        entry.setTotalAmount(asset.getPurchaseCost());
        JournalEntry saved = journalEntryRepository.save(entry);

        // Dr Accumulated Depreciation (remove the contra-asset balance built up so far)
        addLine(saved, asset.getAccumulatedDepreciationAccount(), asset.getAccumulatedDepreciation(), BigDecimal.ZERO,
                "Remove accumulated depreciation - " + asset.getAssetCode());

        // Cr Asset (remove the asset at original cost)
        addLine(saved, asset.getAssetAccount(), BigDecimal.ZERO, asset.getPurchaseCost(),
                "Remove asset at cost - " + asset.getAssetCode());

        // Dr proceeds account (cash/bank received), if any
        if (proceeds.compareTo(BigDecimal.ZERO) > 0) {
            Account proceedsAccount = getAccount(request.getProceedsAccountId());
            addLine(saved, proceedsAccount, proceeds, BigDecimal.ZERO,
                    "Disposal proceeds - " + asset.getAssetCode());
        }

        // Gain (credit) or Loss (debit) on disposal
        if (gainLoss.compareTo(BigDecimal.ZERO) > 0) {
            Account gainLossAccount = getAccount(request.getGainLossAccountId());
            addLine(saved, gainLossAccount, BigDecimal.ZERO, gainLoss,
                    "Gain on disposal - " + asset.getAssetCode());
        } else if (gainLoss.compareTo(BigDecimal.ZERO) < 0) {
            Account gainLossAccount = getAccount(request.getGainLossAccountId());
            addLine(saved, gainLossAccount, gainLoss.abs(), BigDecimal.ZERO,
                    "Loss on disposal - " + asset.getAssetCode());
        }

        asset.setStatus(AssetStatus.DISPOSED);
        asset.setDisposalDate(request.getDisposalDate());
        asset.setDisposalProceeds(proceeds);
        asset.setDisposalGainLoss(gainLoss);

        auditLogService.log(
                AuditAction.DEACTIVATED,
                "FIXED_ASSET",
                asset.getId(),
                AssetStatus.ACTIVE.name(),
                AssetStatus.DISPOSED.name()
        );

        return toResponse(fixedAssetRepository.save(asset));
    }

    // _______ Private helpers __________

    /**
     * Calculates and posts depreciation for one asset up to asOfDate, catching up
     * month-by-month since the last run. Returns empty if nothing is due yet.
     */
    private Optional<DepreciationEntry> runDepreciationInternal(FixedAsset asset, LocalDate asOfDate) {
        LocalDate from = asset.getLastDepreciationDate() != null
                ? asset.getLastDepreciationDate()
                : asset.getPurchaseDate();

        if (!asOfDate.isAfter(from)) {
            return Optional.empty();
        }

        int monthsElapsed = (int) ChronoUnit.MONTHS.between(YearMonth.from(from), YearMonth.from(asOfDate));
        if (monthsElapsed <= 0) {
            return Optional.empty();
        }

        BigDecimal depreciableAmount = asset.depreciableAmount();
        BigDecimal runningAccumulated = asset.getAccumulatedDepreciation();
        BigDecimal totalForRun = BigDecimal.ZERO;

        for (int i = 0; i < monthsElapsed; i++) {
            BigDecimal remaining = depreciableAmount.subtract(runningAccumulated);
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break; // fully depreciated already
            }

            BigDecimal monthly = calcMonthlyDepreciation(asset, runningAccumulated);
            if (monthly.compareTo(remaining) > 0) {
                monthly = remaining;
            }
            if (monthly.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            runningAccumulated = runningAccumulated.add(monthly);
            totalForRun = totalForRun.add(monthly);
        }

        if (totalForRun.compareTo(BigDecimal.ZERO) <= 0) {
            if (asset.getAccumulatedDepreciation().compareTo(depreciableAmount) >= 0) {
                asset.setStatus(AssetStatus.FULLY_DEPRECIATED);
                fixedAssetRepository.save(asset);
            }
            return Optional.empty();
        }

        accountingPeriodService.validatePostingDate(asOfDate);

        JournalEntry entry = new JournalEntry();
        entry.setEntryNumber(generateJournalNumber());
        entry.setDate(asOfDate);
        entry.setDescription("Depreciation - " + asset.getAssetCode() + " - " + asset.getName());
        entry.setType(JournalEntryType.ASSET);
        entry.setStatus(JournalStatus.POSTED);
        entry.setSourceType(JournalSourceType.FIXED_ASSET);
        entry.setSourceId(asset.getId());
        entry.setTotalAmount(totalForRun);
        JournalEntry saved = journalEntryRepository.save(entry);

        addLine(saved, asset.getDepreciationExpenseAccount(), totalForRun, BigDecimal.ZERO,
                "Depreciation expense - " + asset.getAssetCode());
        addLine(saved, asset.getAccumulatedDepreciationAccount(), BigDecimal.ZERO, totalForRun,
                "Accumulated depreciation - " + asset.getAssetCode());

        asset.setAccumulatedDepreciation(runningAccumulated);
        asset.setLastDepreciationDate(asOfDate);
        if (runningAccumulated.compareTo(depreciableAmount) >= 0) {
            asset.setStatus(AssetStatus.FULLY_DEPRECIATED);
        }
        fixedAssetRepository.save(asset);

        DepreciationEntry log = DepreciationEntry.builder()
                .fixedAsset(asset)
                .periodDate(asOfDate)
                .depreciationAmount(totalForRun)
                .accumulatedDepreciationAfter(runningAccumulated)
                .bookValueAfter(asset.getPurchaseCost().subtract(runningAccumulated))
                .journalEntryId(saved.getId())
                .build();

        return Optional.of(depreciationEntryRepository.save(log));
    }

    private BigDecimal calcMonthlyDepreciation(FixedAsset asset, BigDecimal currentAccumulated) {
        if (asset.getDepreciationMethod() == DepreciationMethod.STRAIGHT_LINE) {
            BigDecimal totalMonths = BigDecimal.valueOf(asset.getUsefulLifeYears() * 12L);
            return asset.depreciableAmount().divide(totalMonths, 2, RoundingMode.HALF_UP);
        }

        // REDUCING_BALANCE: monthly rate applied to current book value
        BigDecimal bookValue = asset.getPurchaseCost().subtract(currentAccumulated);
        BigDecimal monthlyRate = asset.getReducingBalanceRate()
                .divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP); // rate% / 12 months
        return bookValue.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
    }

    private void addLine(JournalEntry entry, Account account, BigDecimal debit, BigDecimal credit, String description) {
        JournalLine line = new JournalLine();
        line.setJournalEntry(entry);
        line.setAccount(account);
        line.setDebit(debit);
        line.setCredit(credit);
        line.setDescription(description);
        journalLineRepository.save(line);
        updateBalance(account, debit, credit);

        // If this COA account is linked to a bank account, mirror it into the Banking
        // module too, so Banking/Bank Reconciliation pages stay in sync with the COA.
        if (debit.compareTo(BigDecimal.ZERO) > 0) {
            bankTransactionService.mirrorFromJournal(
                    account.getId(), entry.getDate(), com.nexaerp.banking.enums.TransactionType.CREDIT, debit,
                    description, entry.getEntryNumber(), null,
                    com.nexaerp.banking.enums.TransactionSourceType.FIXED_ASSET, entry.getSourceId());
        } else if (credit.compareTo(BigDecimal.ZERO) > 0) {
            bankTransactionService.mirrorFromJournal(
                    account.getId(), entry.getDate(), com.nexaerp.banking.enums.TransactionType.DEBIT, credit,
                    description, entry.getEntryNumber(), null,
                    com.nexaerp.banking.enums.TransactionSourceType.FIXED_ASSET, entry.getSourceId());
        }
    }

    private void updateBalance(Account account, BigDecimal debit, BigDecimal credit) {
        switch (account.getType()) {
            case ASSET:
            case EXPENSE:
                account.setCurrentBalance(account.getCurrentBalance().add(debit).subtract(credit));
                break;
            case LIABILITY:
            case EQUITY:
            case REVENUE:
                account.setCurrentBalance(account.getCurrentBalance().add(credit).subtract(debit));
                break;
        }
        accountRepository.save(account);
    }

    private String generateJournalNumber() {
        return journalEntryRepository.findTopByOrderByIdDesc()
                .map(last -> {
                    String lastNumber = last.getEntryNumber().replace("JE-", "");
                    int next = Integer.parseInt(lastNumber) + 1;
                    return String.format("JE-%04d", next);
                })
                .orElse("JE-0001");
    }

    private String generateAssetCode() {
        return fixedAssetRepository.findTopByOrderByIdDesc()
                .map(last -> {
                    String lastNumber = last.getAssetCode().replace("FA-", "");
                    int next = Integer.parseInt(lastNumber) + 1;
                    return String.format("FA-%04d", next);
                })
                .orElse("FA-0001");
    }

    private Account getAccount(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));
    }

    private FixedAsset findOrThrow(Long id) {
        return fixedAssetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fixed asset not found"));
    }

    private FixedAssetResponseDto toResponse(FixedAsset a) {
        return FixedAssetResponseDto.builder()
                .id(a.getId())
                .assetCode(a.getAssetCode())
                .name(a.getName())
                .description(a.getDescription())
                .assetAccountId(a.getAssetAccount().getId())
                .assetAccountName(a.getAssetAccount().getName())
                .depreciationExpenseAccountId(a.getDepreciationExpenseAccount().getId())
                .depreciationExpenseAccountName(a.getDepreciationExpenseAccount().getName())
                .accumulatedDepreciationAccountId(a.getAccumulatedDepreciationAccount().getId())
                .accumulatedDepreciationAccountName(a.getAccumulatedDepreciationAccount().getName())
                .purchaseDate(a.getPurchaseDate())
                .purchaseCost(a.getPurchaseCost())
                .salvageValue(a.getSalvageValue())
                .usefulLifeYears(a.getUsefulLifeYears())
                .depreciationMethod(a.getDepreciationMethod())
                .reducingBalanceRate(a.getReducingBalanceRate())
                .accumulatedDepreciation(a.getAccumulatedDepreciation())
                .bookValue(a.bookValue())
                .status(a.getStatus())
                .lastDepreciationDate(a.getLastDepreciationDate())
                .disposalDate(a.getDisposalDate())
                .disposalProceeds(a.getDisposalProceeds())
                .disposalGainLoss(a.getDisposalGainLoss())
                .createdAt(a.getCreatedAt())
                .build();
    }

    private DepreciationEntryResponseDto toDepreciationResponse(DepreciationEntry d) {
        return DepreciationEntryResponseDto.builder()
                .id(d.getId())
                .fixedAssetId(d.getFixedAsset().getId())
                .assetCode(d.getFixedAsset().getAssetCode())
                .assetName(d.getFixedAsset().getName())
                .periodDate(d.getPeriodDate())
                .depreciationAmount(d.getDepreciationAmount())
                .accumulatedDepreciationAfter(d.getAccumulatedDepreciationAfter())
                .bookValueAfter(d.getBookValueAfter())
                .journalEntryId(d.getJournalEntryId())
                .build();
    }
}
