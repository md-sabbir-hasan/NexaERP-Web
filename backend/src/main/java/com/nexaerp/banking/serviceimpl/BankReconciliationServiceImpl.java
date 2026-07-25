package com.nexaerp.banking.serviceimpl;


import com.nexaerp.banking.dto.*;
import com.nexaerp.banking.entity.BankAccount;
import com.nexaerp.banking.entity.BankReconciliation;
import com.nexaerp.banking.entity.BankStatementLine;
import com.nexaerp.banking.entity.BankTransaction;
import com.nexaerp.banking.enums.ReconciliationStatus;
import com.nexaerp.banking.enums.StatementLineStatus;
import com.nexaerp.banking.enums.TransactionType;
import com.nexaerp.banking.repository.BankAccountRepository;
import com.nexaerp.banking.repository.BankReconciliationRepository;
import com.nexaerp.banking.repository.BankStatementLineRepository;
import com.nexaerp.banking.repository.BankTransactionRepository;
import com.nexaerp.banking.services.BankReconciliationService;
import com.nexaerp.banking.services.BankTransactionService;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BankReconciliationServiceImpl implements BankReconciliationService {

    private final BankReconciliationRepository bankReconciliationRepository;
    private final BankAccountRepository bankAccountRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final BankTransactionService bankTransactionService;
    private final BankStatementLineRepository bankStatementLineRepository;

    @Override
    @Transactional
    public BankReconciliationResponseDto start(BankReconciliationStartRequestDto request) {
        BankAccount bankAccount = bankAccountRepository.findById(request.getBankAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Bank account not found"));

        bankReconciliationRepository
                .findByBankAccountIdAndStatus(bankAccount.getId(), ReconciliationStatus.IN_PROGRESS)
                .ifPresent(existing -> {
                    throw new BusinessRuleException(
                            "An in-progress reconciliation (#" + existing.getId() +
                                    ") already exists for this bank account. Complete or reopen-then-complete it first.");
                });

        if (request.getStatementDate() == null) {
            throw new BusinessRuleException("Statement date is required");
        }

        BigDecimal bookBalance = computeBookBalance(bankAccount.getId(), request.getStatementDate());

        BankReconciliation reconciliation = BankReconciliation.builder()
                .bankAccount(bankAccount)
                .statementDate(request.getStatementDate())
                .statementBalance(request.getStatementBalance())
                .bookBalance(bookBalance)
                .status(ReconciliationStatus.IN_PROGRESS)
                .notes(request.getNotes())
                .build();

        BankReconciliation saved = bankReconciliationRepository.save(reconciliation);
        return toResponse(saved);
    }

    @Override
    public BankReconciliationResponseDto getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    public List<BankReconciliationResponseDto> getByBankAccount(Long bankAccountId) {
        return bankReconciliationRepository.findByBankAccountIdOrderByStatementDateDesc(bankAccountId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<BankTransactionResponseDto> getUnmatchedTransactions(Long reconciliationId) {
        BankReconciliation reconciliation = findOrThrow(reconciliationId);
        return findUnmatchedEntities(reconciliation).stream()
                .map(BankTransaction::getId)
                .map(bankTransactionService::getById)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BankReconciliationResponseDto matchTransactions(Long reconciliationId, List<Long> transactionIds) {
        BankReconciliation reconciliation = findOrThrow(reconciliationId);
        assertInProgress(reconciliation);

        for (Long txnId : transactionIds) {
            BankTransaction transaction = bankTransactionRepository.findById(txnId)
                    .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + txnId));

            if (!transaction.getBankAccount().getId().equals(reconciliation.getBankAccount().getId())) {
                throw new BusinessRuleException(
                        "Transaction " + transaction.getTransactionNumber() + " does not belong to this bank account");
            }
            if (Boolean.TRUE.equals(transaction.getVoided())) {
                throw new BusinessRuleException(
                        "Transaction " + transaction.getTransactionNumber() + " is voided and cannot be matched");
            }
            if (Boolean.TRUE.equals(transaction.getReconciled())) {
                throw new BusinessRuleException(
                        "Transaction " + transaction.getTransactionNumber() + " is already reconciled");
            }
            if (transaction.getTransactionDate().isAfter(reconciliation.getStatementDate())) {
                throw new BusinessRuleException(
                        "Transaction " + transaction.getTransactionNumber() +
                                " is dated after the statement date and cannot be matched to this statement");
            }

            transaction.setReconciled(true);
            transaction.setReconciledAt(LocalDateTime.now());
            transaction.setReconciliationId(reconciliation.getId());
            bankTransactionRepository.save(transaction);
        }

        return toResponse(reconciliation);
    }

    @Override
    @Transactional
    public BankReconciliationResponseDto unmatchTransaction(Long reconciliationId, Long transactionId) {
        BankReconciliation reconciliation = findOrThrow(reconciliationId);
        assertInProgress(reconciliation);

        BankTransaction transaction = bankTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (!reconciliation.getId().equals(transaction.getReconciliationId())) {
            throw new BusinessRuleException("This transaction is not matched to this reconciliation");
        }

        transaction.setReconciled(false);
        transaction.setReconciledAt(null);
        transaction.setReconciliationId(null);
        bankTransactionRepository.save(transaction);

        return toResponse(reconciliation);
    }

    @Override
    @Transactional
    public BankReconciliationResponseDto addAdjustment(Long reconciliationId, BankTransactionRequestDto request) {
        BankReconciliation reconciliation = findOrThrow(reconciliationId);
        createAdjustmentTransaction(reconciliation, request);
        return toResponse(reconciliation);
    }

    // Creates the real BankTransaction (+ Journal Entry, via the existing transaction service)
    // for something the bank statement shows that isn't booked yet, and immediately reconciles it.
    private BankTransaction createAdjustmentTransaction(
            BankReconciliation reconciliation, BankTransactionRequestDto request) {
        assertInProgress(reconciliation);

        if (request.getTransactionDate() != null
                && request.getTransactionDate().isAfter(reconciliation.getStatementDate())) {
            throw new BusinessRuleException(
                    "Adjustment date cannot be after the statement date (" + reconciliation.getStatementDate() + ")");
        }

        // Adjustment always belongs to this reconciliation's bank account, regardless of what was passed in
        request.setBankAccountId(reconciliation.getBankAccount().getId());

        BankTransactionResponseDto created = bankTransactionService.create(request);

        BankTransaction entity = bankTransactionRepository.findById(created.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found after creation"));
        entity.setReconciled(true);
        entity.setReconciledAt(LocalDateTime.now());
        entity.setReconciliationId(reconciliation.getId());
        return bankTransactionRepository.save(entity);
    }

    @Override
    @Transactional
    public BankReconciliationResponseDto complete(Long reconciliationId) {
        BankReconciliation reconciliation = findOrThrow(reconciliationId);
        assertInProgress(reconciliation);

        BankReconciliationResponseDto summary = toResponse(reconciliation);

        if (summary.getDifference().compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessRuleException(
                    "Book and bank balances don't tie out yet. Difference = " + summary.getDifference() +
                            ". Match remaining transactions or add missing adjustments before completing.");
        }

        reconciliation.setStatus(ReconciliationStatus.COMPLETED);
        reconciliation.setCompletedAt(LocalDateTime.now());
        BankReconciliation saved = bankReconciliationRepository.save(reconciliation);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public BankReconciliationResponseDto reopen(Long reconciliationId) {
        BankReconciliation reconciliation = findOrThrow(reconciliationId);

        if (reconciliation.getStatus() != ReconciliationStatus.COMPLETED) {
            throw new BusinessRuleException("Only a completed reconciliation can be reopened");
        }

        reconciliation.setStatus(ReconciliationStatus.IN_PROGRESS);
        reconciliation.setCompletedAt(null);
        BankReconciliation saved = bankReconciliationRepository.save(reconciliation);

        return toResponse(saved);
    }

    // _______ CSV statement import __________

    private static final DateTimeFormatter[] DATE_FORMATS = new DateTimeFormatter[] {
            DateTimeFormatter.ISO_LOCAL_DATE,          // 2026-07-31
            DateTimeFormatter.ofPattern("dd/MM/yyyy"), // 31/07/2026
            DateTimeFormatter.ofPattern("dd-MM-yyyy"), // 31-07-2026
            DateTimeFormatter.ofPattern("MM/dd/yyyy"), // 07/31/2026
    };

    @Override
    @Transactional
    public StatementImportResultDto importStatement(Long reconciliationId, MultipartFile file) {
        BankReconciliation reconciliation = findOrThrow(reconciliationId);
        assertInProgress(reconciliation);

        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("CSV file is required");
        }

        List<BankStatementLine> parsedLines = parseCsv(reconciliation, file);
        if (parsedLines.isEmpty()) {
            throw new BusinessRuleException("No valid rows found in the uploaded CSV");
        }

        // Candidates already booked in the system, still unmatched, for this account/period
        List<BankTransaction> candidates = findUnmatchedEntities(reconciliation);
        Set<Long> consumed = new HashSet<>();

        int autoMatched = 0;
        for (BankStatementLine line : parsedLines) {
            BankTransaction best = null;
            for (BankTransaction candidate : candidates) {
                if (consumed.contains(candidate.getId())) continue;
                if (candidate.getTransactionType() != line.getTransactionType()) continue;
                if (candidate.getAmount().compareTo(line.getAmount()) != 0) continue;
                best = candidate;
                // Prefer an exact date match if one exists; otherwise take the first amount/type match
                if (candidate.getTransactionDate().isEqual(line.getLineDate())) break;
            }

            if (best != null) {
                consumed.add(best.getId());
                best.setReconciled(true);
                best.setReconciledAt(LocalDateTime.now());
                best.setReconciliationId(reconciliation.getId());
                bankTransactionRepository.save(best);

                line.setStatus(StatementLineStatus.MATCHED);
                line.setMatchedTransactionId(best.getId());
                autoMatched++;
            }

            bankStatementLineRepository.save(line);
        }

        List<BankStatementLineResponseDto> lineDtos = getStatementLines(reconciliationId);

        return StatementImportResultDto.builder()
                .totalLines(parsedLines.size())
                .autoMatchedCount(autoMatched)
                .unmatchedCount(parsedLines.size() - autoMatched)
                .lines(lineDtos)
                .reconciliation(toResponse(reconciliation))
                .build();
    }

    private List<BankStatementLine> parseCsv(BankReconciliation reconciliation, MultipartFile file) {
        List<BankStatementLine> lines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String header = reader.readLine();
            if (header == null) {
                return lines;
            }

            header = header.strip().replace("\uFEFF", "");

            List<String> columns = splitCsvRow(header).stream()
                    .map(c -> c.trim().toLowerCase())
                    .collect(Collectors.toList());

            int dateIdx = columns.indexOf("date");
            int descIdx = columns.indexOf("description");
            int amountIdx = columns.indexOf("amount");
            int typeIdx = columns.indexOf("type");
            int refIdx = columns.indexOf("reference");

            if (dateIdx == -1 || amountIdx == -1) {
                throw new BusinessRuleException(
                        "CSV must have at least 'Date' and 'Amount' columns (optionally Description, Type, Reference)");
            }

            String row;
            int rowNum = 1;
            while ((row = reader.readLine()) != null) {
                rowNum++;
                if (row.isBlank()) continue;

                List<String> cells = splitCsvRow(row);
                if (cells.size() <= amountIdx || cells.size() <= dateIdx) continue;

                LocalDate date = parseDate(cells.get(dateIdx).trim());
                if (date == null) {
                    throw new BusinessRuleException("Row " + rowNum + ": unrecognized date '" + cells.get(dateIdx) + "'");
                }

                BigDecimal rawAmount;
                try {
                    rawAmount = new BigDecimal(cells.get(amountIdx).trim().replace(",", ""));
                } catch (NumberFormatException e) {
                    throw new BusinessRuleException("Row " + rowNum + ": invalid amount '" + cells.get(amountIdx) + "'");
                }

                TransactionType type;
                if (typeIdx != -1 && typeIdx < cells.size() && !cells.get(typeIdx).isBlank()) {
                    type = TransactionType.valueOf(cells.get(typeIdx).trim().toUpperCase());
                } else {
                    // No Type column: infer from sign (+ = money in = CREDIT, - = money out = DEBIT)
                    type = rawAmount.compareTo(BigDecimal.ZERO) < 0 ? TransactionType.DEBIT : TransactionType.CREDIT;
                }

                BankStatementLine line = BankStatementLine.builder()
                        .reconciliation(reconciliation)
                        .lineDate(date)
                        .description(descIdx != -1 && descIdx < cells.size() ? cells.get(descIdx).trim() : null)
                        .amount(rawAmount.abs())
                        .transactionType(type)
                        .referenceNumber(refIdx != -1 && refIdx < cells.size() ? cells.get(refIdx).trim() : null)
                        .status(StatementLineStatus.UNMATCHED)
                        .build();

                lines.add(line);
            }
        } catch (IOException e) {
            throw new BusinessRuleException("Could not read the CSV file: " + e.getMessage());
        }

        return lines;
    }

    // Minimal CSV splitter: handles simple comma-separated values and basic "quoted, values"
    private List<String> splitCsvRow(String row) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (char c : row.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result;
    }

    private LocalDate parseDate(String raw) {
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(raw, fmt);
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }
        return null;
    }

    @Override
    public List<BankStatementLineResponseDto> getStatementLines(Long reconciliationId) {
        return bankStatementLineRepository.findByReconciliationIdOrderByLineDateAsc(reconciliationId).stream()
                .map(this::toLineResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BankStatementLineResponseDto matchStatementLine(Long reconciliationId, Long lineId, Long transactionId) {
        BankReconciliation reconciliation = findOrThrow(reconciliationId);
        assertInProgress(reconciliation);

        BankStatementLine line = bankStatementLineRepository.findById(lineId)
                .orElseThrow(() -> new ResourceNotFoundException("Statement line not found"));
        if (!line.getReconciliation().getId().equals(reconciliationId)) {
            throw new BusinessRuleException("This statement line does not belong to this reconciliation");
        }
        if (line.getStatus() == StatementLineStatus.MATCHED) {
            throw new BusinessRuleException("This statement line is already matched");
        }

        // Reuses the same validation as the manual multi-match endpoint
        matchTransactions(reconciliationId, List.of(transactionId));

        line.setStatus(StatementLineStatus.MATCHED);
        line.setMatchedTransactionId(transactionId);
        BankStatementLine saved = bankStatementLineRepository.save(line);

        return toLineResponse(saved);
    }

    @Override
    @Transactional
    public BankStatementLineResponseDto convertLineToAdjustment(
            Long reconciliationId, Long lineId, Long contraAccountId, String descriptionOverride) {
        BankReconciliation reconciliation = findOrThrow(reconciliationId);
        assertInProgress(reconciliation);

        BankStatementLine line = bankStatementLineRepository.findById(lineId)
                .orElseThrow(() -> new ResourceNotFoundException("Statement line not found"));
        if (!line.getReconciliation().getId().equals(reconciliationId)) {
            throw new BusinessRuleException("This statement line does not belong to this reconciliation");
        }
        if (line.getStatus() == StatementLineStatus.MATCHED) {
            throw new BusinessRuleException("This statement line is already matched");
        }

        BankTransactionRequestDto request = new BankTransactionRequestDto();
        request.setBankAccountId(reconciliation.getBankAccount().getId());
        request.setTransactionType(line.getTransactionType());
        request.setTransactionDate(line.getLineDate());
        request.setAmount(line.getAmount());
        request.setContraAccountId(contraAccountId);
        request.setReferenceNumber(line.getReferenceNumber());
        request.setDescription(
                (descriptionOverride != null && !descriptionOverride.isBlank())
                        ? descriptionOverride
                        : line.getDescription());

        // addAdjustment() also re-validates IN_PROGRESS + date, creates the BankTransaction/Journal
        // Entry and reconciles it against this batch.
        BankTransaction created = createAdjustmentTransaction(reconciliation, request);

        line.setStatus(StatementLineStatus.MATCHED);
        line.setMatchedTransactionId(created.getId());
        BankStatementLine saved = bankStatementLineRepository.save(line);

        return toLineResponse(saved);
    }

    private BankStatementLineResponseDto toLineResponse(BankStatementLine line) {
        String matchedTxnNumber = null;
        if (line.getMatchedTransactionId() != null) {
            matchedTxnNumber = bankTransactionRepository.findById(line.getMatchedTransactionId())
                    .map(BankTransaction::getTransactionNumber)
                    .orElse(null);
        }

        return BankStatementLineResponseDto.builder()
                .id(line.getId())
                .reconciliationId(line.getReconciliation().getId())
                .lineDate(line.getLineDate())
                .description(line.getDescription())
                .amount(line.getAmount())
                .transactionType(line.getTransactionType())
                .referenceNumber(line.getReferenceNumber())
                .status(line.getStatus())
                .matchedTransactionId(line.getMatchedTransactionId())
                .matchedTransactionNumber(matchedTxnNumber)
                .build();
    }

    // _______ Private helpers __________

    private BankReconciliation findOrThrow(Long id) {
        return bankReconciliationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reconciliation not found"));
    }

    private void assertInProgress(BankReconciliation reconciliation) {
        if (reconciliation.getStatus() != ReconciliationStatus.IN_PROGRESS) {
            throw new BusinessRuleException(
                    "Reconciliation #" + reconciliation.getId() + " is already completed. Reopen it first.");
        }
    }

    private BigDecimal computeBookBalance(Long bankAccountId, java.time.LocalDate asOfDate) {
        BankAccount bankAccount = bankAccountRepository.findById(bankAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Bank account not found"));
        BigDecimal netMovement = bankTransactionRepository.sumNetMovementUpTo(bankAccountId, asOfDate);
        return bankAccount.getOpeningBalance().add(netMovement);
    }

    private List<BankTransaction> findUnmatchedEntities(BankReconciliation reconciliation) {
        return bankTransactionRepository
                .findByBankAccountIdAndReconciledFalseAndVoidedFalseAndTransactionDateLessThanEqual(
                        reconciliation.getBankAccount().getId(), reconciliation.getStatementDate());
    }

    private BankReconciliationResponseDto toResponse(BankReconciliation r) {
        BigDecimal bookBalance = computeBookBalance(r.getBankAccount().getId(), r.getStatementDate());

        List<BankTransaction> unmatched = findUnmatchedEntities(r);

        BigDecimal depositsInTransit = unmatched.stream()
                .filter(t -> t.getTransactionType() == TransactionType.CREDIT)
                .map(BankTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal outstandingCheques = unmatched.stream()
                .filter(t -> t.getTransactionType() == TransactionType.DEBIT)
                .map(BankTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal adjustedBankBalance = r.getStatementBalance()
                .add(depositsInTransit)
                .subtract(outstandingCheques);

        BigDecimal difference = bookBalance.subtract(adjustedBankBalance);

        return BankReconciliationResponseDto.builder()
                .id(r.getId())
                .bankAccountId(r.getBankAccount().getId())
                .bankAccountName(r.getBankAccount().getAccountName())
                .statementDate(r.getStatementDate())
                .statementBalance(r.getStatementBalance())
                .bookBalance(bookBalance)
                .depositsInTransit(depositsInTransit)
                .outstandingCheques(outstandingCheques)
                .adjustedBankBalance(adjustedBankBalance)
                .difference(difference)
                .status(r.getStatus())
                .notes(r.getNotes())
                .completedAt(r.getCompletedAt())
                .createdAt(r.getCreatedAt())
                .unmatchedTransactionIds(unmatched.stream().map(BankTransaction::getId).collect(Collectors.toList()))
                .build();
    }
}
