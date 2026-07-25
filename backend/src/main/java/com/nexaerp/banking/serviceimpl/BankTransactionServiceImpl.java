package com.nexaerp.banking.serviceimpl;


import com.nexaerp.account.Account;
import com.nexaerp.account.AccountRepository;
import com.nexaerp.accountingperiod.AccountingPeriodService;
import com.nexaerp.banking.dto.BankTransactionRequestDto;
import com.nexaerp.banking.dto.BankTransactionResponseDto;
import com.nexaerp.banking.dto.BankTransferRequestDto;
import com.nexaerp.banking.dto.BankTransferResponseDto;
import com.nexaerp.banking.entity.BankAccount;
import com.nexaerp.banking.entity.BankTransaction;
import com.nexaerp.banking.enums.TransactionSourceType;
import com.nexaerp.banking.enums.TransactionType;
import com.nexaerp.banking.repository.BankAccountRepository;
import com.nexaerp.banking.repository.BankTransactionRepository;
import com.nexaerp.banking.services.BankTransactionService;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.journal.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BankTransactionServiceImpl implements BankTransactionService {

    private final BankTransactionRepository bankTransactionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalLineRepository journalLineRepository;
    private final AccountingPeriodService accountingPeriodService;


    @Override
    @Transactional
    public BankTransactionResponseDto create(BankTransactionRequestDto request) {
        BankAccount bankAccount = bankAccountRepository.findById(request.getBankAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Bank account not found"));

        if (!Boolean.TRUE.equals(bankAccount.getIsActive())) {
            throw new BusinessRuleException("Cannot post a transaction to an inactive bank account");
        }

        Account contraAccount = accountRepository.findById(request.getContraAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Contra account not found"));

        BankTransaction saved = createInternal(
                bankAccount,
                request.getTransactionDate(),
                request.getTransactionType(),
                request.getAmount(),
                request.getDescription(),
                request.getReferenceNumber(),
                contraAccount,
                TransactionSourceType.MANUAL,
                null
        );

        return toResponse(saved);
    }

    @Override
    public BankTransactionResponseDto getById(Long id) {
        BankTransaction transaction = bankTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        return toResponse(transaction);
    }

    @Override
    public List<BankTransactionResponseDto> getAll() {
        return bankTransactionRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<BankTransactionResponseDto> getByAccount(Long bankAccountId) {
        return bankTransactionRepository.findByBankAccountId(bankAccountId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<BankTransactionResponseDto> getByAccountAndDateRange(Long bankAccountId, LocalDate from, LocalDate to) {
        return bankTransactionRepository
                .findByBankAccountIdAndTransactionDateBetween(bankAccountId, from, to)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BankTransactionResponseDto reconcile(Long id) {
        BankTransaction transaction = bankTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (Boolean.TRUE.equals(transaction.getVoided())) {
            throw new BusinessRuleException("Cannot reconcile a voided transaction");
        }

        if (transaction.getReconciled()) {
            throw new BusinessRuleException("Transaction already reconciled");
        }

        transaction.setReconciled(true);
        transaction.setReconciledAt(LocalDateTime.now());

        return toResponse(bankTransactionRepository.save(transaction));
    }

    @Override
    @Transactional
    public BankTransactionResponseDto unreconcile(Long id) {
        BankTransaction transaction = bankTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (Boolean.TRUE.equals(transaction.getVoided())) {
            throw new BusinessRuleException("Cannot un-reconcile a voided transaction");
        }

        if (!Boolean.TRUE.equals(transaction.getReconciled())) {
            throw new BusinessRuleException("Transaction is not reconciled");
        }

        transaction.setReconciled(false);
        transaction.setReconciledAt(null);

        return toResponse(bankTransactionRepository.save(transaction));
    }

    @Override
    @Transactional
    public BankTransactionResponseDto voidTransaction(Long id) {
        BankTransaction transaction = bankTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (Boolean.TRUE.equals(transaction.getVoided())) {
            throw new BusinessRuleException("Transaction is already voided");
        }

        if (Boolean.TRUE.equals(transaction.getReconciled())) {
            throw new BusinessRuleException(
                    "Cannot void a reconciled transaction. Un-reconcile it first.");
        }

        LocalDate reversalDate = LocalDate.now();

        /*
         * Validate before reversing bank balance,
         * COA balance or journal entry.
         */
        accountingPeriodService.validatePostingDate(reversalDate);


        BankAccount bankAccount = transaction.getBankAccount();

        // Reverse the bank account balance effect
        if (transaction.getTransactionType() == TransactionType.CREDIT) {
            bankAccount.setCurrentBalance(
                    bankAccount.getCurrentBalance().subtract(transaction.getAmount()));
        } else {
            bankAccount.setCurrentBalance(
                    bankAccount.getCurrentBalance().add(transaction.getAmount()));
        }
        bankAccountRepository.save(bankAccount);

        // Reverse the linked journal entry (Dr/Cr swapped)
        journalEntryRepository
                .findBySourceTypeAndSourceId(JournalSourceType.BANK_TRANSACTION, transaction.getId())
                .ifPresent(original -> {
                    if (original.getStatus() == JournalStatus.REVERSED) {
                        throw new BusinessRuleException("Journal entry is already reversed");
                    }

                    JournalEntry reversal = new JournalEntry();
                    reversal.setEntryNumber(generateJournalNumber());
                    reversal.setDate(LocalDate.now());
                    reversal.setDescription("Void - " + transaction.getTransactionNumber());
                    reversal.setType(JournalEntryType.BANK);
                    reversal.setStatus(JournalStatus.POSTED);
                    reversal.setSourceType(JournalSourceType.BANK_TRANSACTION);
                    reversal.setSourceId(transaction.getId());
                    reversal.setTotalAmount(original.getTotalAmount());
                    reversal.setReversedFromId(original.getId());
                    reversal.setReferenceNumber("REV-" + original.getReferenceNumber());

                    JournalEntry savedReversal = journalEntryRepository.save(reversal);

                    List<JournalLine> originalLines =
                            journalLineRepository.findByJournalEntryId(original.getId());

                    originalLines.forEach(line ->
                            saveLineAndUpdateBalance(savedReversal, line.getAccount(),
                                    line.getCredit(), line.getDebit())); // swapped

                    original.setStatus(JournalStatus.REVERSED);
                    journalEntryRepository.save(original);
                });

        transaction.setVoided(true);
        transaction.setVoidedAt(LocalDateTime.now());

        return toResponse(bankTransactionRepository.save(transaction));
    }

    @Override
    @Transactional
    public BankTransferResponseDto transfer(BankTransferRequestDto request) {
        if (request.getFromBankAccountId().equals(request.getToBankAccountId())) {
            throw new BusinessRuleException("Source and destination bank accounts must be different");
        }

        BankAccount fromAccount = bankAccountRepository.findById(request.getFromBankAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Source bank account not found"));

        BankAccount toAccount = bankAccountRepository.findById(request.getToBankAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination bank account not found"));

        if (!Boolean.TRUE.equals(fromAccount.getIsActive())) {
            throw new BusinessRuleException("Source bank account is inactive");
        }
        if (!Boolean.TRUE.equals(toAccount.getIsActive())) {
            throw new BusinessRuleException("Destination bank account is inactive");
        }

        Account fromCoa = accountRepository.findById(fromAccount.getCoaAccountId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "COA account not found for source bank account"));
        Account toCoa = accountRepository.findById(toAccount.getCoaAccountId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "COA account not found for destination bank account"));

        String description = (request.getDescription() != null && !request.getDescription().isBlank())
                ? request.getDescription()
                : "Transfer: " + fromAccount.getAccountName() + " -> " + toAccount.getAccountName();

        // Debit leg on the source account (money going out), contra = destination's COA
        BankTransaction debitLeg = createInternal(
                fromAccount,
                request.getTransactionDate(),
                TransactionType.DEBIT,
                request.getAmount(),
                description,
                request.getReferenceNumber(),
                toCoa,
                TransactionSourceType.TRANSFER,
                null
        );

        // Credit leg on the destination account (money coming in), contra = source's COA
        BankTransaction creditLeg = createInternal(
                toAccount,
                request.getTransactionDate(),
                TransactionType.CREDIT,
                request.getAmount(),
                description,
                request.getReferenceNumber(),
                fromCoa,
                TransactionSourceType.TRANSFER,
                debitLeg.getId()
        );

        // Link the debit leg to the credit leg as well, for traceability
        debitLeg.setSourceId(creditLeg.getId());
        bankTransactionRepository.save(debitLeg);

        return BankTransferResponseDto.builder()
                .debitTransaction(toResponse(debitLeg))
                .creditTransaction(toResponse(creditLeg))
                .build();
    }

    @Override
    @Transactional
    public Optional<BankTransactionResponseDto> mirrorFromJournal(
            Long coaAccountId,
            LocalDate date,
            TransactionType type,
            BigDecimal amount,
            String description,
            String referenceNumber,
            Long contraAccountId,
            TransactionSourceType sourceType,
            Long sourceId
    ) {
        Optional<BankAccount> linked = bankAccountRepository.findByCoaAccountId(coaAccountId);
        if (linked.isEmpty()) {
            return Optional.empty(); // not a bank-linked account, nothing to mirror
        }
        BankAccount bankAccount = linked.get();

        BankTransaction transaction = BankTransaction.builder()
                .transactionNumber(generateTransactionNumber())
                .bankAccount(bankAccount)
                .transactionDate(date)
                .transactionType(type)
                .amount(amount)
                .description(description)
                .referenceNumber(referenceNumber)
                .contraAccountId(contraAccountId)
                .reconciled(false)
                .voided(false)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .build();

        BankTransaction saved = bankTransactionRepository.save(transaction);

        if (type == TransactionType.CREDIT) {
            bankAccount.setCurrentBalance(bankAccount.getCurrentBalance().add(amount));
        } else {
            bankAccount.setCurrentBalance(bankAccount.getCurrentBalance().subtract(amount));
        }
        bankAccountRepository.save(bankAccount);

        // Deliberately no createJournalEntry() call here — the caller already posted the
        // COA-side journal entry; this only mirrors it into the Banking module.

        return Optional.of(toResponse(saved));
    }



    // _______Private helper__________


    private BankTransaction createInternal(
            BankAccount bankAccount,
            LocalDate transactionDate,
            TransactionType transactionType,
            BigDecimal amount,
            String description,
            String referenceNumber,
            Account contraAccount,
            TransactionSourceType sourceType,
            Long sourceId
    ) {
        if (transactionDate == null) {
            throw new BusinessRuleException(
                    "Transaction date is required"
            );
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException(
                    "Transaction amount must be greater than zero"
            );
        }

        /*
         * Must run before:
         * - BankTransaction save
         * - Bank balance update
         * - Journal creation
         * - COA balance update
         */
        accountingPeriodService.validatePostingDate(transactionDate);

        BankTransaction transaction = BankTransaction.builder()
                .transactionNumber(generateTransactionNumber())
                .bankAccount(bankAccount)
                .transactionDate(transactionDate)
                .transactionType(transactionType)
                .amount(amount)
                .description(description)
                .referenceNumber(referenceNumber)
                .contraAccountId(contraAccount.getId())
                .reconciled(false)
                .voided(false)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .build();

        BankTransaction saved =
                bankTransactionRepository.save(transaction);

        if (transactionType == TransactionType.CREDIT) {
            bankAccount.setCurrentBalance(
                    bankAccount.getCurrentBalance().add(amount)
            );
        } else {
            bankAccount.setCurrentBalance(
                    bankAccount.getCurrentBalance().subtract(amount)
            );
        }

        bankAccountRepository.save(bankAccount);

        createJournalEntry(saved, bankAccount, contraAccount);

        return saved;
    }

    private void createJournalEntry(BankTransaction transaction,
                                    BankAccount bankAccount,
                                    Account contraAccount) {

        // Get COA account linked to this bank account
        Account bankCoaAccount = accountRepository.findById(bankAccount.getCoaAccountId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "COA account not found for bank account"));

        JournalEntry entry = new JournalEntry();
        entry.setEntryNumber(generateJournalNumber());
        entry.setDate(transaction.getTransactionDate());
        entry.setDescription(transaction.getDescription());
        entry.setType(JournalEntryType.BANK);
        entry.setStatus(JournalStatus.POSTED);
        entry.setSourceType(JournalSourceType.BANK_TRANSACTION);
        entry.setSourceId(transaction.getId());
        entry.setReferenceNumber(transaction.getTransactionNumber());
        entry.setTotalAmount(transaction.getAmount());

        JournalEntry saved = journalEntryRepository.save(entry);

        if (transaction.getTransactionType() == TransactionType.CREDIT) {
            // Money coming in -> Dr Bank COA, Cr Contra Account
            saveLineAndUpdateBalance(saved, bankCoaAccount, transaction.getAmount(), BigDecimal.ZERO);
            saveLineAndUpdateBalance(saved, contraAccount, BigDecimal.ZERO, transaction.getAmount());
        } else {
            // Money going out -> Dr Contra Account, Cr Bank COA
            saveLineAndUpdateBalance(saved, contraAccount, transaction.getAmount(), BigDecimal.ZERO);
            saveLineAndUpdateBalance(saved, bankCoaAccount, BigDecimal.ZERO, transaction.getAmount());
        }
    }

    private void saveLineAndUpdateBalance(JournalEntry entry, Account account,
                                          BigDecimal debit, BigDecimal credit) {
        JournalLine line = new JournalLine();
        line.setJournalEntry(entry);
        line.setAccount(account);
        line.setDebit(debit);
        line.setCredit(credit);
        line.setDescription(entry.getDescription());
        journalLineRepository.save(line);

        updateBalance(account, debit, credit);
    }

    private void updateBalance(Account account, BigDecimal debit, BigDecimal credit) {
        switch (account.getType()) {
            case ASSET:
            case EXPENSE:
                account.setCurrentBalance(
                        account.getCurrentBalance().add(debit).subtract(credit));
                break;
            case LIABILITY:
            case EQUITY:
            case REVENUE:
                account.setCurrentBalance(
                        account.getCurrentBalance().add(credit).subtract(debit));
                break;
        }
        accountRepository.save(account);
    }

    private synchronized String generateTransactionNumber() {
        int year = Year.now().getValue();
        return bankTransactionRepository.findTopByOrderByIdDesc()
                .map(last -> {
                    String[] parts = last.getTransactionNumber().split("-");
                    int next = Integer.parseInt(parts[2]) + 1;
                    return String.format("TXN-%d-%06d", year, next);
                })
                .orElse(String.format("TXN-%d-%06d", year, 1));
    }

    private synchronized String generateJournalNumber() {
        return journalEntryRepository.findTopByOrderByIdDesc()
                .map(last -> {
                    String lastNumber = last.getEntryNumber().replace("JE-", "");
                    int next = Integer.parseInt(lastNumber) + 1;
                    return String.format("JE-%04d", next);
                })
                .orElse("JE-0001");
    }

    private BankTransactionResponseDto toResponse(BankTransaction transaction) {

        String contraAccountName = null;
        if (transaction.getContraAccountId() != null) {
            contraAccountName = accountRepository.findById(transaction.getContraAccountId())
                    .map(a -> a.getName())
                    .orElse(null);
        }

        return BankTransactionResponseDto.builder()
                .id(transaction.getId())
                .transactionNumber(transaction.getTransactionNumber())
                .bankAccountId(transaction.getBankAccount().getId())
                .bankAccountName(transaction.getBankAccount().getAccountName())
                .transactionDate(transaction.getTransactionDate())
                .transactionType(transaction.getTransactionType())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .referenceNumber(transaction.getReferenceNumber())
                .contraAccountId(transaction.getContraAccountId())
                .contraAccountName(contraAccountName)
                .reconciled(transaction.getReconciled())
                .reconciledAt(transaction.getReconciledAt())
                .voided(transaction.getVoided())
                .voidedAt(transaction.getVoidedAt())
                .sourceType(transaction.getSourceType())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}