package com.nexaerp.journal;

import com.nexaerp.account.Account;
import com.nexaerp.account.AccountRepository;
import com.nexaerp.account.AccountType;
import com.nexaerp.accountingperiod.AccountingPeriodService;
import com.nexaerp.audit.AuditAction;
import com.nexaerp.audit.AuditLogService;
import com.nexaerp.banking.enums.TransactionSourceType;
import com.nexaerp.banking.enums.TransactionType;
import com.nexaerp.banking.services.BankTransactionService;
import com.nexaerp.budget.BudgetCheckService;
import com.nexaerp.budget.dto.BudgetWarningDto;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.costcenter.CostCenter;
import com.nexaerp.costcenter.CostCenterService;
import com.nexaerp.email.BudgetAlertEmailService;
import com.nexaerp.journal.dto.JournalEntryRequestDto;
import com.nexaerp.journal.dto.JournalEntryResponseDto;
import com.nexaerp.journal.dto.JournalLineRequestDto;
import com.nexaerp.journal.dto.JournalLineResponseDto;
import com.nexaerp.security.MakerCheckerService;
import com.nexaerp.notification.NotificationService;
import com.nexaerp.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JournalEntryServiceImpl implements JournalEntryService{

    private final JournalEntryRepository journalEntryRepository;
    private final JournalLineRepository journalLineRepository;
    private final AccountRepository accountRepository;
    private final AuditLogService auditLogService;
    private final AccountingPeriodService accountingPeriodService;
    private final MakerCheckerService makerCheckerService;
    private final BankTransactionService bankTransactionService;
    private final BudgetCheckService budgetCheckService;
    private final NotificationService notificationService;
    private final BudgetAlertEmailService budgetAlertEmailService;
    private final CostCenterService costCenterService;


    @Override
    @Transactional
    public JournalEntryResponseDto create(JournalEntryRequestDto request) {
        validateLines(request.getLines());

        JournalEntry entry = new JournalEntry();
        entry.setEntryNumber(generateEntryNumber());
        entry.setDate(request.getDate());
        entry.setDescription(request.getDescription());
        entry.setType(request.getType());
        entry.setStatus(JournalStatus.DRAFT);
        entry.setSourceType(JournalSourceType.MANUAL);
        entry.setReferenceNumber(entry.getEntryNumber());


        // Total amount calculate
        BigDecimal total = request.getLines().stream()
                .map(JournalLineRequestDto::getDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        entry.setTotalAmount(total);

        JournalEntry saved = journalEntryRepository.save(entry);

        // Lines save
        List<JournalLine> lines = request.getLines().stream()
                .map(lineDto -> buildLine(lineDto, saved))
                .collect(Collectors.toList());
        journalLineRepository.saveAll(lines);
        saved.setLines(lines);

        auditLogService.log(
                AuditAction.CREATED,
                "JOURNAL_ENTRY",
                saved.getId(),
                null,
                saved.getEntryNumber()
        );

        return toResponse(saved);
    }

    @Override
    @Transactional
    public JournalEntryResponseDto update(Long id, JournalEntryRequestDto request) {
        JournalEntry entry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Journal entry not found"));

        if (!entry.getStatus().equals(JournalStatus.DRAFT)) {
            throw new BusinessRuleException("Only DRAFT entries can be updated");
        }

        validateLines(request.getLines());

        entry.setDate(request.getDate());
        entry.setDescription(request.getDescription());
        entry.setType(request.getType());

        // remove older line, replace new line
        journalLineRepository.deleteAll(entry.getLines());

        BigDecimal total = request.getLines().stream()
                .map(JournalLineRequestDto::getDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        entry.setTotalAmount(total);

        JournalEntry saved = journalEntryRepository.save(entry);

        List<JournalLine> lines = request.getLines().stream()
                .map(lineDto -> buildLine(lineDto, saved))
                .collect(Collectors.toList());
        journalLineRepository.saveAll(lines);
        saved.setLines(lines);

        auditLogService.log(
                AuditAction.UPDATED,
                "JOURNAL_ENTRY",
                saved.getId(),
                null,
                saved.getEntryNumber()
        );

        return toResponse(saved);
    }

    @Override
    public JournalEntryResponseDto getById(Long id) {
        JournalEntry entry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Journal entry not found"));
        return toResponse(entry);
    }

    @Override
    public List<JournalEntryResponseDto> getAll() {
        return journalEntryRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public JournalEntryResponseDto post(Long id) {
        JournalEntry entry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Journal entry not found"));

        if (entry.getStatus() == JournalStatus.POSTED) {
            throw new BusinessRuleException("Journal entry is already posted");
        }

        if (entry.getStatus() == JournalStatus.REVERSED) {
            throw new BusinessRuleException("Cannot post a reversed journal entry");
        }

        if (!entry.getStatus().equals(JournalStatus.DRAFT)) {
            throw new BusinessRuleException("Only DRAFT entries can be posted");
        }

        if (entry.getLines() == null || entry.getLines().isEmpty()) {
            throw new BusinessRuleException("Cannot post a journal entry with zero lines");
        }

        BigDecimal totalDebit = entry.getLines().stream()
                .map(JournalLine::getDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredit = entry.getLines().stream()
                .map(JournalLine::getCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new BusinessRuleException("Total debit must equal total credit before posting");
        }

//        Maker-Checker
        makerCheckerService.validateChecker(
                entry.getCreatedBy(),
                "Journal Entry"
        );

        // Validate Accounting Period
        accountingPeriodService.validatePostingDate(entry.getDate());
        entry.getLines().forEach(line -> validateActiveCostCenter(line.getCostCenter()));


        // Balance update
        for (JournalLine line : entry.getLines()) {
            updateAccountBalance(line);
        }

        entry.setStatus(JournalStatus.POSTED);
        JournalEntry posted = journalEntryRepository.save(entry);

        auditLogService.log(
                AuditAction.POSTED,
                "JOURNAL_ENTRY",
                posted.getId(),
                "DRAFT",
                "POSTED"
        );

        JournalEntry completed = journalEntryRepository.save(entry);
        List<BudgetWarningDto> budgetWarnings = checkBudgets(completed);
        budgetAlertEmailService.scheduleAfterCommit(
                "Journal",
                completed.getId(),
                completed.getEntryNumber(),
                completed.getDate(),
                budgetWarnings
        );
        budgetWarnings.forEach(this::createBudgetExceededNotification);
        return toResponse(completed);
    }

    @Override
    @Transactional
    public JournalEntryResponseDto reverse(Long id) {
        JournalEntry original = journalEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Journal entry not found"));

        if (original.getSourceType() != JournalSourceType.MANUAL) {
            throw new BusinessRuleException(
                    "Only manual journal entries can be reversed from Journal Register. Please reverse the source document instead."
            );
        }

        if (original.getStatus() == JournalStatus.REVERSED) {
            throw new BusinessRuleException("Journal entry is already reversed");
        }

        if (!original.getStatus().equals(JournalStatus.POSTED)) {
            throw new BusinessRuleException("Only POSTED entries can be reversed");
        }

        //period_validation
        LocalDate reversalDate = LocalDate.now();

        accountingPeriodService.validatePostingDate(reversalDate);

        JournalEntry reversal = new JournalEntry();
        reversal.setEntryNumber(generateEntryNumber());
        reversal.setDate(reversalDate);
        reversal.setDescription("Reversal of " + original.getEntryNumber());
        reversal.setType(original.getType());
        reversal.setStatus(JournalStatus.POSTED);
        reversal.setSourceType(JournalSourceType.MANUAL);
        reversal.setReversedFromId(original.getId());
        reversal.setReferenceNumber("REV-" + original.getEntryNumber());
        reversal.setTotalAmount(original.getTotalAmount());

        JournalEntry savedReversal = journalEntryRepository.save(reversal);

        List<JournalLine> reversalLines = original.getLines().stream()
                .map(line -> JournalLine.builder()
                        .journalEntry(savedReversal)
                        .account(line.getAccount())
                        .costCenter(line.getCostCenter())
                        .debit(line.getCredit())
                        .credit(line.getDebit())
                        .description("Reversal: " + line.getDescription())
                        .build())
                .collect(Collectors.toList());

        journalLineRepository.saveAll(reversalLines);
        savedReversal.setLines(reversalLines);

        for (JournalLine line : reversalLines) {
            updateAccountBalance(line);
        }

        original.setStatus(JournalStatus.REVERSED);
        journalEntryRepository.save(original);

        auditLogService.log(
                AuditAction.REVERSED,
                "JOURNAL_ENTRY",
                original.getId(),
                "POSTED",
                "REVERSED"
        );

        return toResponse(savedReversal);
    }

    @Override
    @Transactional
    public void delete(Long id) {

        JournalEntry entry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Journal entry not found"));

        if (!entry.getStatus().equals(JournalStatus.DRAFT)) {
            throw new BusinessRuleException("Only DRAFT entries can be deleted");
        }

        journalEntryRepository.delete(entry);

        auditLogService.log(
                AuditAction.DELETED,
                "JOURNAL_ENTRY",
                id,
                entry.getEntryNumber(),
                null
        );

        journalEntryRepository.delete(entry);

    }

    private List<BudgetWarningDto> checkBudgets(JournalEntry entry) {
        Map<Long, BigDecimal> debitByAccount = new LinkedHashMap<>();
        Map<Long, Account> accounts = new LinkedHashMap<>();

        for (JournalLine line : entry.getLines()) {
            Account account = line.getAccount();
            if (account.getType() != AccountType.EXPENSE
                    || line.getDebit().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            accounts.putIfAbsent(account.getId(), account);
            debitByAccount.merge(account.getId(), line.getDebit(), BigDecimal::add);
        }

        return debitByAccount.entrySet().stream()
                .map(accountDebit -> budgetCheckService.checkExpenseAccount(
                        accounts.get(accountDebit.getKey()),
                        entry.getDate(),
                        accountDebit.getValue()
                ))
                .flatMap(java.util.Optional::stream)
                .collect(Collectors.toList());
    }

    private void createBudgetExceededNotification(BudgetWarningDto warning) {
        String route = warning.getBudgetId() != null
                ? "/budget/" + warning.getBudgetId() + "/variance"
                : "/budget";
        String message = String.format(
                "Budget for %s exceeded by %s.",
                warning.getAccountName(),
                warning.getExceededAmount()
        );

        try {
            notificationService.createForCurrentUser(
                    NotificationType.BUDGET_EXCEEDED,
                    "Budget exceeded",
                    message,
                    route,
                    "BUDGET",
                    warning.getBudgetId()
            );
        } catch (RuntimeException exception) {
            log.warn(
                    "Journal posting succeeded, but budget notification creation failed for budget {} and account {}",
                    warning.getBudgetId(),
                    warning.getAccountId(),
                    exception
            );
        }
    }
                                // -- Private Helpers --

    private void validateLines(List<JournalLineRequestDto> lines) {

        BigDecimal totalDebit = lines.stream()
                .map(JournalLineRequestDto::getDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredit = lines.stream()
                .map(JournalLineRequestDto::getCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new BusinessRuleException(
                    "Total debit (" + totalDebit + ") must equal total credit (" + totalCredit + ")"
            );
        }
        // "Every line must contain either a debit or a credit, never both together"
        for (JournalLineRequestDto line : lines) {
            if (line.getDebit().compareTo(BigDecimal.ZERO) > 0
                    && line.getCredit().compareTo(BigDecimal.ZERO) > 0) {
                throw new BusinessRuleException("A line cannot have both debit and credit");
            }
            if (line.getDebit().compareTo(BigDecimal.ZERO) == 0
                    && line.getCredit().compareTo(BigDecimal.ZERO) == 0) {
                throw new BusinessRuleException("A line must have either debit or credit");
            }
        }
    }

    private void updateAccountBalance(JournalLine line) {

        Account account = line.getAccount();

        switch (account.getType()) {
            case ASSET:
            case EXPENSE:
                account.setCurrentBalance(
                        account.getCurrentBalance()
                                .add(line.getDebit())
                                .subtract(line.getCredit())
                );
                break;
            case LIABILITY:
            case EQUITY:
            case REVENUE:
                account.setCurrentBalance(
                        account.getCurrentBalance()
                                .add(line.getCredit())
                                .subtract(line.getDebit())
                );
                break;
        }

        accountRepository.save(account);

        // If this COA account is linked to a bank account, mirror it into the Banking
        // module too, so Banking/Bank Reconciliation pages stay in sync with the COA
        // even for manually-entered journal entries.
        JournalEntry entry = line.getJournalEntry();
        if (line.getDebit().compareTo(BigDecimal.ZERO) > 0) {
            bankTransactionService.mirrorFromJournal(
                    account.getId(), entry.getDate(), TransactionType.CREDIT, line.getDebit(),
                    entry.getDescription(), entry.getEntryNumber(), null,
                    TransactionSourceType.JOURNAL, entry.getId());
        } else if (line.getCredit().compareTo(BigDecimal.ZERO) > 0) {
            bankTransactionService.mirrorFromJournal(
                    account.getId(), entry.getDate(), TransactionType.DEBIT, line.getCredit(),
                    entry.getDescription(), entry.getEntryNumber(), null,
                    TransactionSourceType.JOURNAL, entry.getId());
        }
    }

    private String generateEntryNumber() {
        return journalEntryRepository.findTopByOrderByIdDesc()
                .map(last -> {
                    String lastNumber = last.getEntryNumber().replace("JE-", "");
                    int next = Integer.parseInt(lastNumber) + 1;
                    return String.format("JE-%04d", next);
                })
                .orElse("JE-0001");
    }

    private JournalLine buildLine(JournalLineRequestDto dto, JournalEntry entry) {
        Account account = accountRepository.findById(dto.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + dto.getAccountId()));

        if (!account.getIsActive()) {
            throw new BusinessRuleException("Cannot use inactive account: " + account.getCode());
        }

        if (accountRepository.existsByParentId(account.getId())) {
            throw new BusinessRuleException("Cannot post journal to parent account: " + account.getCode());
        }
        CostCenter costCenter = costCenterService.resolveActive(dto.getCostCenterId());

        return JournalLine.builder()
                .journalEntry(entry)
                .account(account)
                .costCenter(costCenter)
                .debit(dto.getDebit())
                .credit(dto.getCredit())
                .description(dto.getDescription())
                .build();
    }

                                     // -- Mappers --

    private JournalEntryResponseDto toResponse(JournalEntry entry) {
        JournalEntryResponseDto dto = JournalEntryResponseDto.builder()
                .id(entry.getId())
                .entryNumber(entry.getEntryNumber())
                .date(entry.getDate())
                .description(entry.getDescription())
                .type(entry.getType())
                .status(entry.getStatus())
                .sourceType(entry.getSourceType())
                .totalAmount(entry.getTotalAmount())
                .build();

        if (entry.getLines() != null) {
            dto.setLines(entry.getLines().stream()
                    .map(this::toLineResponse)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    private JournalLineResponseDto toLineResponse(JournalLine line) {
        return JournalLineResponseDto.builder()
                .id(line.getId())
                .accountId(line.getAccount().getId())
                .accountName(line.getAccount().getName())
                .accountCode(line.getAccount().getCode())
                .costCenterId(line.getCostCenter() != null ? line.getCostCenter().getId() : null)
                .costCenterCode(line.getCostCenter() != null ? line.getCostCenter().getCode() : null)
                .costCenterName(line.getCostCenter() != null ? line.getCostCenter().getName() : null)
                .debit(line.getDebit())
                .credit(line.getCredit())
                .description(line.getDescription())
                .build();
    }

    private void validateActiveCostCenter(CostCenter costCenter) {
        if (costCenter != null) {
            costCenterService.resolveActive(costCenter.getId());
        }
    }
}
