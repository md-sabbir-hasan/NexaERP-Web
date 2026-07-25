package com.nexaerp.banking.services;

import com.nexaerp.banking.dto.BankReconciliationResponseDto;
import com.nexaerp.banking.dto.BankReconciliationStartRequestDto;
import com.nexaerp.banking.dto.BankStatementLineResponseDto;
import com.nexaerp.banking.dto.BankTransactionRequestDto;
import com.nexaerp.banking.dto.BankTransactionResponseDto;
import com.nexaerp.banking.dto.StatementImportResultDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BankReconciliationService {

    // Start a new reconciliation session for a bank account + statement period
    BankReconciliationResponseDto start(BankReconciliationStartRequestDto request);

    BankReconciliationResponseDto getById(Long id);

    List<BankReconciliationResponseDto> getByBankAccount(Long bankAccountId);

    // Transactions still eligible to be matched (unreconciled, dated on/before the statement date)
    List<BankTransactionResponseDto> getUnmatchedTransactions(Long reconciliationId);

    // Mark a batch of existing transactions as cleared/matched against this statement
    BankReconciliationResponseDto matchTransactions(Long reconciliationId, List<Long> transactionIds);

    // Undo a match (only while the reconciliation is still IN_PROGRESS)
    BankReconciliationResponseDto unmatchTransaction(Long reconciliationId, Long transactionId);

    // Record something the bank statement shows but the books don't have yet
    // (bank charge, interest, direct debit, etc.) — creates the BankTransaction + Journal Entry
    // and immediately marks it reconciled against this batch.
    BankReconciliationResponseDto addAdjustment(Long reconciliationId, BankTransactionRequestDto request);

    // Lock the reconciliation. Fails if book balance and statement balance don't tie out.
    BankReconciliationResponseDto complete(Long reconciliationId);

    // Re-open a completed reconciliation (e.g. an error was found later)
    BankReconciliationResponseDto reopen(Long reconciliationId);

    // ---- CSV statement import ----

    // Parses the uploaded CSV into BankStatementLine rows and tries to auto-match
    // each one against this reconciliation's still-unmatched BankTransactions
    // (same bank account, same type, exact amount, closest date).
    StatementImportResultDto importStatement(Long reconciliationId, MultipartFile file);

    List<BankStatementLineResponseDto> getStatementLines(Long reconciliationId);

    // Manually tie a still-UNMATCHED statement line to an existing unmatched BankTransaction
    BankStatementLineResponseDto matchStatementLine(Long reconciliationId, Long lineId, Long transactionId);

    // Statement line has no matching book entry at all (e.g. bank charge) ->
    // create it as a real adjustment transaction and mark the line MATCHED
    BankStatementLineResponseDto convertLineToAdjustment(
            Long reconciliationId, Long lineId, Long contraAccountId, String descriptionOverride);
}
