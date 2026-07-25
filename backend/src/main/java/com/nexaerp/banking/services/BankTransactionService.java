package com.nexaerp.banking.services;

import com.nexaerp.banking.dto.BankTransactionRequestDto;
import com.nexaerp.banking.dto.BankTransactionResponseDto;
import com.nexaerp.banking.dto.BankTransferRequestDto;
import com.nexaerp.banking.dto.BankTransferResponseDto;
import com.nexaerp.banking.enums.TransactionSourceType;
import com.nexaerp.banking.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BankTransactionService {
    BankTransactionResponseDto create(BankTransactionRequestDto request);
    BankTransactionResponseDto getById(Long id);
    List<BankTransactionResponseDto> getAll();
    List<BankTransactionResponseDto> getByAccount(Long bankAccountId);
    List<BankTransactionResponseDto> getByAccountAndDateRange(
            Long bankAccountId, LocalDate from, LocalDate to);
    BankTransactionResponseDto reconcile(Long id);
    BankTransactionResponseDto unreconcile(Long id);
    BankTransactionResponseDto voidTransaction(Long id);
    BankTransferResponseDto transfer(BankTransferRequestDto request);

    // ---- Cross-module bank sync ----
    //
    // Called by OTHER modules (Payment, Fixed Asset, manual Journal Entry, ...) right after
    // THEY have already posted their own journal line against a COA account. If that COA
    // account happens to be linked to a BankAccount, this creates the matching BankTransaction
    // row (so it shows up in Banking/Bank Reconciliation) and updates BankAccount.currentBalance.
    //
    // Does NOT create a journal entry — the caller already did that. Returns empty if the
    // account is not bank-linked (safe no-op for normal, non-bank COA accounts).
    Optional<BankTransactionResponseDto> mirrorFromJournal(
            Long coaAccountId,
            LocalDate date,
            TransactionType type,
            BigDecimal amount,
            String description,
            String referenceNumber,
            Long contraAccountId,
            TransactionSourceType sourceType,
            Long sourceId
    );
}