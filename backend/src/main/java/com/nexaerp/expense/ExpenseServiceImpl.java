package com.nexaerp.expense;

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
import com.nexaerp.expense.dto.ExpenseCancelRequestDto;
import com.nexaerp.expense.dto.ExpenseRequestDto;
import com.nexaerp.expense.dto.ExpenseResponseDto;
import com.nexaerp.journal.*;
import com.nexaerp.notification.NotificationService;
import com.nexaerp.notification.NotificationType;
import com.nexaerp.party.Party;
import com.nexaerp.party.PartyRepository;
import com.nexaerp.payment.PaymentAllocation;
import com.nexaerp.payment.PaymentAllocationRepository;
import com.nexaerp.payment.PaymentReferenceType;
import com.nexaerp.settings.SettingKey;
import com.nexaerp.settings.SystemSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final AccountRepository accountRepository;
    private final PartyRepository partyRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalLineRepository journalLineRepository;
    private final AccountingPeriodService accountingPeriodService;
    private final SystemSettingsService systemSettingsService;
    private final BankTransactionService bankTransactionService;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final AuditLogService auditLogService;
    private final BudgetCheckService budgetCheckService;
    private final NotificationService notificationService;
    private final BudgetAlertEmailService budgetAlertEmailService;
    private final CostCenterService costCenterService;

    @Override
    @Transactional
    public ExpenseResponseDto create(ExpenseRequestDto request) {
        return createInternal(request, null);
    }

    @Override
    @Transactional
    public ExpenseResponseDto createFromRecurringTemplate(ExpenseRequestDto request, Long recurringTemplateId) {
        return createInternal(request, recurringTemplateId);
    }

    private ExpenseResponseDto createInternal(ExpenseRequestDto request, Long recurringTemplateId) {

        Account expenseAccount = getAccount(request.getExpenseAccountId());
        if (expenseAccount.getType() != AccountType.EXPENSE) {
            throw new BusinessRuleException("Expense category account must be of type EXPENSE");
        }

        boolean paidImmediately = Boolean.TRUE.equals(request.getPaidImmediately());
        CostCenter costCenter = costCenterService.resolveActive(request.getCostCenterId());

        Party party = null;
        if (request.getPartyId() != null) {
            party = getParty(request.getPartyId());
        }

        Account paymentAccount = null;
        if (paidImmediately) {
            if (request.getPaymentAccountId() == null) {
                throw new BusinessRuleException("paymentAccountId is required when paidImmediately = true");
            }
            paymentAccount = getAccount(request.getPaymentAccountId());
            if (paymentAccount.getId().equals(expenseAccount.getId())) {
                throw new BusinessRuleException("Payment account cannot be the same as the expense account");
            }
        } else if (party == null) {
            throw new BusinessRuleException("partyId is required when paidImmediately = false (pay later)");
        }

        boolean isFromRecurring = recurringTemplateId != null;

        // Recurring-generated expenses land as DRAFT ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â no journal entry, no money
        // movement ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â until someone explicitly clicks "Post". Manual entries (this
        // method called with recurringTemplateId = null) keep posting immediately,
        // since the person is already reviewing it as they submit the form.
        if (isFromRecurring) {
            Expense draft = Expense.builder()
                    .expenseNumber(generateExpenseNumber())
                    .expenseDate(request.getExpenseDate())
                    .expenseAccount(expenseAccount)
                    .costCenter(costCenter)
                    .paidImmediately(paidImmediately)
                    .paymentAccount(paymentAccount)
                    .party(party)
                    .amount(request.getAmount())
                    .paidAmount(BigDecimal.ZERO)
                    .dueAmount(BigDecimal.ZERO)
                    .paymentStatus(ExpensePaymentStatus.UNPAID)
                    .referenceNumber(request.getReferenceNumber())
                    .attachmentUrl(request.getAttachmentUrl())
                    .notes(request.getNotes())
                    .status(ExpenseStatus.DRAFT)
                    .recurringTemplateId(recurringTemplateId)
                    .build();

            Expense saved = expenseRepository.save(draft);

            auditLogService.log(
                    AuditAction.CREATED,
                    "EXPENSE",
                    saved.getId(),
                    null,
                    saved.getExpenseNumber() + " - DRAFT (awaiting review from recurring template)"
            );

            return toResponse(saved, Collections.emptyList());
        }

        accountingPeriodService.validatePostingDate(request.getExpenseDate());

        Expense expense = Expense.builder()
                .expenseNumber(generateExpenseNumber())
                .expenseDate(request.getExpenseDate())
                .expenseAccount(expenseAccount)
                .costCenter(costCenter)
                .paidImmediately(paidImmediately)
                .paymentAccount(paymentAccount)
                .party(party)
                .amount(request.getAmount())
                .paidAmount(paidImmediately ? request.getAmount() : BigDecimal.ZERO)
                .dueAmount(paidImmediately ? BigDecimal.ZERO : request.getAmount())
                .paymentStatus(paidImmediately ? ExpensePaymentStatus.PAID : ExpensePaymentStatus.UNPAID)
                .referenceNumber(request.getReferenceNumber())
                .attachmentUrl(request.getAttachmentUrl())
                .notes(request.getNotes())
                .status(ExpenseStatus.POSTED)
                .recurringTemplateId(null)
                .build();

        Expense saved = expenseRepository.save(expense);

        Account creditAccount = paidImmediately
                ? paymentAccount
                : systemSettingsService.getAccount(SettingKey.DEFAULT_PAYABLE_ACCOUNT);

        postJournalForExpense(saved, creditAccount);

        auditLogService.log(
                AuditAction.CREATED,
                "EXPENSE",
                saved.getId(),
                null,
                saved.getExpenseNumber() + " - " + saved.getAmount()
        );

        List<BudgetWarningDto> budgetWarnings = budgetCheckService
                .checkExpenseAccount(expenseAccount, request.getExpenseDate(), request.getAmount())
                .map(List::of)
                .orElse(Collections.emptyList());

        budgetAlertEmailService.scheduleAfterCommit(
                "Expense",
                saved.getId(),
                saved.getExpenseNumber(),
                saved.getExpenseDate(),
                budgetWarnings
        );
        notifyBudgetExceeded(budgetWarnings);
        return toResponse(saved, budgetWarnings);
    }

    @Override
    @Transactional
    public ExpenseResponseDto post(Long id) {
        Expense expense = findOrThrow(id);

        if (expense.getStatus() != ExpenseStatus.DRAFT) {
            throw new BusinessRuleException("Only a DRAFT expense can be posted");
        }

        accountingPeriodService.validatePostingDate(expense.getExpenseDate());
        validateActiveCostCenter(expense.getCostCenter());

        boolean paidImmediately = Boolean.TRUE.equals(expense.getPaidImmediately());

        Account creditAccount = paidImmediately
                ? expense.getPaymentAccount()
                : systemSettingsService.getAccount(SettingKey.DEFAULT_PAYABLE_ACCOUNT);

        postJournalForExpense(expense, creditAccount);

        expense.setStatus(ExpenseStatus.POSTED);
        expense.setPaidAmount(paidImmediately ? expense.getAmount() : BigDecimal.ZERO);
        expense.setDueAmount(paidImmediately ? BigDecimal.ZERO : expense.getAmount());
        expense.setPaymentStatus(paidImmediately ? ExpensePaymentStatus.PAID : ExpensePaymentStatus.UNPAID);

        Expense saved = expenseRepository.save(expense);

        auditLogService.log(
                AuditAction.POSTED,
                "EXPENSE",
                saved.getId(),
                ExpenseStatus.DRAFT.name(),
                ExpenseStatus.POSTED.name()
        );

        List<BudgetWarningDto> budgetWarnings = budgetCheckService
                .checkExpenseAccount(expense.getExpenseAccount(), expense.getExpenseDate(), expense.getAmount())
                .map(List::of)
                .orElse(Collections.emptyList());

        budgetAlertEmailService.scheduleAfterCommit(
                "Expense",
                saved.getId(),
                saved.getExpenseNumber(),
                saved.getExpenseDate(),
                budgetWarnings
        );
        notifyBudgetExceeded(budgetWarnings);
        return toResponse(saved, budgetWarnings);
    }

    @Override
    public ExpenseResponseDto getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    public List<ExpenseResponseDto> getAll() {
        return expenseRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ExpenseResponseDto cancel(Long id, ExpenseCancelRequestDto request) {
        Expense expense = findOrThrow(id);

        if (expense.getStatus() == ExpenseStatus.CANCELLED) {
            throw new BusinessRuleException("Expense " + expense.getExpenseNumber() + " is already cancelled");
        }

        if (expense.getStatus() == ExpenseStatus.DRAFT) {
            // Nothing was posted yet ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â no journal to reverse, just mark it cancelled
            expense.setStatus(ExpenseStatus.CANCELLED);
            expense.setCancelledAt(LocalDateTime.now());
            expense.setCancelReason(request.getReason());
            Expense saved = expenseRepository.save(expense);

            auditLogService.log(
                    AuditAction.CANCELLED,
                    "EXPENSE",
                    saved.getId(),
                    ExpenseStatus.DRAFT.name(),
                    ExpenseStatus.CANCELLED.name()
            );

            return toResponse(saved);
        }

        // If money has already been paid against this expense via the Payment module
        // (the "pay later" case), it must be un-allocated / that Payment cancelled first.
        List<PaymentAllocation> existingAllocations =
                paymentAllocationRepository.findByReferenceTypeAndReferenceId(
                        PaymentReferenceType.EXPENSE, expense.getId());
        if (!existingAllocations.isEmpty()) {
            throw new BusinessRuleException(
                    "Cannot cancel ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â payment(s) already recorded against this expense. Cancel those payments first.");
        }

        journalEntryRepository.findBySourceTypeAndSourceId(JournalSourceType.EXPENSE_CLAIM, expense.getId())
                .filter(original -> original.getStatus() != JournalStatus.REVERSED)
                .ifPresent(original -> {
                    JournalEntry reversal = new JournalEntry();
                    reversal.setEntryNumber(generateJournalNumber());
                    reversal.setDate(LocalDate.now());
                    reversal.setDescription("Reversal - " + expense.getExpenseNumber());
                    reversal.setType(original.getType());
                    reversal.setStatus(JournalStatus.POSTED);
                    reversal.setSourceType(JournalSourceType.EXPENSE_CLAIM);
                    reversal.setSourceId(expense.getId());
                    reversal.setTotalAmount(original.getTotalAmount());
                    reversal.setReversedFromId(original.getId());
                    reversal.setReferenceNumber("REV-" + original.getEntryNumber());
                    JournalEntry savedReversal = journalEntryRepository.save(reversal);

                    List<JournalLine> originalLines = journalLineRepository.findByJournalEntryId(original.getId());
                    originalLines.forEach(line ->
                            addLine(savedReversal, line.getAccount(), line.getCostCenter(),
                                    line.getCredit(), line.getDebit(),
                                    "Reversal - " + expense.getExpenseNumber())); // debit/credit swapped

                    original.setStatus(JournalStatus.REVERSED);
                    journalEntryRepository.save(original);
                });

        expense.setStatus(ExpenseStatus.CANCELLED);
        expense.setCancelledAt(LocalDateTime.now());
        expense.setCancelReason(request.getReason());
        expense.setPaidAmount(BigDecimal.ZERO);
        expense.setDueAmount(BigDecimal.ZERO);
        expense.setPaymentStatus(ExpensePaymentStatus.UNPAID);

        Expense saved = expenseRepository.save(expense);

        auditLogService.log(
                AuditAction.CANCELLED,
                "EXPENSE",
                saved.getId(),
                ExpenseStatus.POSTED.name(),
                ExpenseStatus.CANCELLED.name()
        );

        return toResponse(saved);
    }

    @Override
    @Transactional
    public ExpenseResponseDto attachReceipt(Long id, String attachmentUrl) {
        Expense expense = findOrThrow(id);
        expense.setAttachmentUrl(attachmentUrl);
        return toResponse(expenseRepository.save(expense), Collections.emptyList());
    }

    private void notifyBudgetExceeded(List<BudgetWarningDto> warnings) {
        Set<String> notifiedBudgets = new HashSet<>();

        for (BudgetWarningDto warning : warnings) {
            String deduplicationKey = warning.getBudgetId() + ":" + warning.getAccountId();
            if (!notifiedBudgets.add(deduplicationKey)) {
                continue;
            }

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
                        "Expense succeeded, but budget notification creation failed for budget {} and account {}",
                        warning.getBudgetId(),
                        warning.getAccountId(),
                        exception
                );
            }
        }
    }
    // _______ Private helpers __________

    private void postJournalForExpense(Expense expense, Account creditAccount) {
        boolean paidImmediately = Boolean.TRUE.equals(expense.getPaidImmediately());

        JournalEntry entry = new JournalEntry();
        entry.setEntryNumber(generateJournalNumber());
        entry.setDate(expense.getExpenseDate());
        entry.setDescription("Expense - " + expense.getExpenseNumber() + " - " + expense.getExpenseAccount().getName());
        entry.setType(paidImmediately ? JournalEntryType.CASH : JournalEntryType.GENERAL);
        entry.setStatus(JournalStatus.POSTED);
        entry.setSourceType(JournalSourceType.EXPENSE_CLAIM);
        entry.setSourceId(expense.getId());
        entry.setTotalAmount(expense.getAmount());
        entry.setReferenceNumber(expense.getExpenseNumber());
        JournalEntry savedEntry = journalEntryRepository.save(entry);

        addLine(savedEntry, expense.getExpenseAccount(), expense.getCostCenter(), expense.getAmount(), BigDecimal.ZERO,
                "Expense - " + expense.getExpenseNumber());
        addLine(savedEntry, creditAccount, null, BigDecimal.ZERO, expense.getAmount(),
                "Expense - " + expense.getExpenseNumber());
    }

    private void addLine(JournalEntry entry, Account account, CostCenter costCenter,
                         BigDecimal debit, BigDecimal credit, String description) {
        JournalLine line = new JournalLine();
        line.setJournalEntry(entry);
        line.setAccount(account);
        line.setCostCenter(costCenter);
        line.setDebit(debit);
        line.setCredit(credit);
        line.setDescription(description);
        journalLineRepository.save(line);
        updateBalance(account, debit, credit);

        // Mirror into Banking module if this COA account is linked to a BankAccount
        if (debit.compareTo(BigDecimal.ZERO) > 0) {
            bankTransactionService.mirrorFromJournal(
                    account.getId(), entry.getDate(), TransactionType.CREDIT, debit,
                    description, entry.getEntryNumber(), null,
                    TransactionSourceType.EXPENSE, entry.getSourceId());
        } else if (credit.compareTo(BigDecimal.ZERO) > 0) {
            bankTransactionService.mirrorFromJournal(
                    account.getId(), entry.getDate(), TransactionType.DEBIT, credit,
                    description, entry.getEntryNumber(), null,
                    TransactionSourceType.EXPENSE, entry.getSourceId());
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

    private String generateExpenseNumber() {
        return expenseRepository.findTopByOrderByIdDesc()
                .map(last -> {
                    String lastNumber = last.getExpenseNumber().replace("EXP-", "");
                    int next = Integer.parseInt(lastNumber) + 1;
                    return String.format("EXP-%04d", next);
                })
                .orElse("EXP-0001");
    }

    private Account getAccount(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));
    }

    private Party getParty(Long id) {
        return partyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Party not found: " + id));
    }

    private Expense findOrThrow(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
    }

    private void validateActiveCostCenter(CostCenter costCenter) {
        if (costCenter != null) {
            costCenterService.resolveActive(costCenter.getId());
        }
    }

    private ExpenseResponseDto toResponse(Expense e) {
        return toResponse(e, Collections.emptyList());
    }

    private ExpenseResponseDto toResponse(Expense e, List<BudgetWarningDto> budgetWarnings) {
        return ExpenseResponseDto.builder()
                .id(e.getId())
                .expenseNumber(e.getExpenseNumber())
                .expenseDate(e.getExpenseDate())
                .expenseAccountId(e.getExpenseAccount().getId())
                .expenseAccountName(e.getExpenseAccount().getName())
                .costCenterId(e.getCostCenter() != null ? e.getCostCenter().getId() : null)
                .costCenterCode(e.getCostCenter() != null ? e.getCostCenter().getCode() : null)
                .costCenterName(e.getCostCenter() != null ? e.getCostCenter().getName() : null)
                .paidImmediately(e.getPaidImmediately())
                .paymentAccountId(e.getPaymentAccount() != null ? e.getPaymentAccount().getId() : null)
                .paymentAccountName(e.getPaymentAccount() != null ? e.getPaymentAccount().getName() : null)
                .partyId(e.getParty() != null ? e.getParty().getId() : null)
                .partyName(e.getParty() != null ? e.getParty().getName() : null)
                .amount(e.getAmount())
                .paidAmount(e.getPaidAmount())
                .dueAmount(e.getDueAmount())
                .paymentStatus(e.getPaymentStatus())
                .referenceNumber(e.getReferenceNumber())
                .attachmentUrl(e.getAttachmentUrl())
                .notes(e.getNotes())
                .status(e.getStatus())
                .cancelledAt(e.getCancelledAt())
                .cancelReason(e.getCancelReason())
                .createdAt(e.getCreatedAt())
                .recurringTemplateId(e.getRecurringTemplateId())
                .budgetWarnings(budgetWarnings)
                .build();
    }
}
