package com.nexaerp.recurringexpense.serviceimpl;

import com.nexaerp.account.Account;
import com.nexaerp.account.AccountRepository;
import com.nexaerp.account.AccountType;
import com.nexaerp.audit.AuditAction;
import com.nexaerp.audit.AuditLogService;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.expense.ExpenseService;
import com.nexaerp.expense.dto.ExpenseRequestDto;
import com.nexaerp.expense.dto.ExpenseResponseDto;
import com.nexaerp.party.Party;
import com.nexaerp.party.PartyRepository;
import com.nexaerp.recurringexpense.*;
import com.nexaerp.recurringexpense.dto.RecurringExpenseTemplateRequestDto;
import com.nexaerp.recurringexpense.dto.RecurringExpenseTemplateResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecurringExpenseTemplateServiceImpl implements RecurringExpenseTemplateService {

    private final RecurringExpenseTemplateRepository recurringExpenseTemplateRepository;
    private final AccountRepository accountRepository;
    private final PartyRepository partyRepository;
    private final ExpenseService expenseService;
    private final AuditLogService auditLogService;

    // Self-injection so that self.generateSingleTemplate(...) goes through the Spring proxy
    // and @Transactional actually applies — plain internal calls (this.xxx()) bypass AOP.
    // Same pattern already used in SystemSettingsServiceImpl.
    @Lazy
    @Autowired
    private RecurringExpenseTemplateService self;

    @Override
    @Transactional
    public RecurringExpenseTemplateResponseDto create(RecurringExpenseTemplateRequestDto request) {

        Account expenseAccount = getAccount(request.getExpenseAccountId());
        if (expenseAccount.getType() != AccountType.EXPENSE) {
            throw new BusinessRuleException("Category account must be of type EXPENSE");
        }

        Account paymentAccount = null;
        Party party = null;

        if (request.getPartyId() != null) {
            party = getParty(request.getPartyId());
        }

        if (Boolean.TRUE.equals(request.getPaidImmediately())) {
            if (request.getPaymentAccountId() == null) {
                throw new BusinessRuleException("paymentAccountId is required when paidImmediately = true");
            }
            paymentAccount = getAccount(request.getPaymentAccountId());
        } else if (party == null) {
            throw new BusinessRuleException("partyId is required when paidImmediately = false");
        }

        if (request.getEndDate() != null && !request.getEndDate().isAfter(request.getStartDate())) {
            throw new BusinessRuleException("endDate must be after startDate");
        }

        RecurringExpenseTemplate template = RecurringExpenseTemplate.builder()
                .name(request.getName())
                .expenseAccount(expenseAccount)
                .amount(request.getAmount())
                .paidImmediately(request.getPaidImmediately())
                .paymentAccount(paymentAccount)
                .party(party)
                .frequency(request.getFrequency())
                .startDate(request.getStartDate())
                .nextRunDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(RecurringExpenseStatus.ACTIVE)
                .referenceNumber(request.getReferenceNumber())
                .notes(request.getNotes())
                .build();

        RecurringExpenseTemplate saved = recurringExpenseTemplateRepository.save(template);

        auditLogService.log(
                AuditAction.CREATED,
                "RECURRING_EXPENSE_TEMPLATE",
                saved.getId(),
                null,
                saved.getName() + " - " + saved.getFrequency()
        );

        return toResponse(saved);
    }

    @Override
    @Transactional
    public RecurringExpenseTemplateResponseDto update(Long id, RecurringExpenseTemplateRequestDto request) {
        RecurringExpenseTemplate template = findOrThrow(id);

        Account expenseAccount = getAccount(request.getExpenseAccountId());
        if (expenseAccount.getType() != AccountType.EXPENSE) {
            throw new BusinessRuleException("Category account must be of type EXPENSE");
        }

        Account paymentAccount = null;
        Party party = null;

        if (request.getPartyId() != null) {
            party = getParty(request.getPartyId());
        }

        if (Boolean.TRUE.equals(request.getPaidImmediately())) {
            if (request.getPaymentAccountId() == null) {
                throw new BusinessRuleException("paymentAccountId is required when paidImmediately = true");
            }
            paymentAccount = getAccount(request.getPaymentAccountId());
        } else if (party == null) {
            throw new BusinessRuleException("partyId is required when paidImmediately = false");
        }

        template.setName(request.getName());
        template.setExpenseAccount(expenseAccount);
        template.setAmount(request.getAmount());
        template.setPaidImmediately(request.getPaidImmediately());
        template.setPaymentAccount(paymentAccount);
        template.setParty(party);
        template.setFrequency(request.getFrequency());
        template.setStartDate(request.getStartDate());
        template.setEndDate(request.getEndDate());
        template.setReferenceNumber(request.getReferenceNumber());
        template.setNotes(request.getNotes());

        // Only reset the schedule if this template has never generated an expense yet —
        // otherwise editing details shouldn't disrupt an already-progressing schedule.
        if (template.getLastGeneratedDate() == null) {
            template.setNextRunDate(request.getStartDate());
        }

        return toResponse(recurringExpenseTemplateRepository.save(template));
    }

    @Override
    public RecurringExpenseTemplateResponseDto getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    public List<RecurringExpenseTemplateResponseDto> getAll() {
        return recurringExpenseTemplateRepository.findByDeletedAtIsNullOrderByCreatedAtDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        RecurringExpenseTemplate template = findOrThrow(id);
        template.setDeletedAt(java.time.LocalDateTime.now());
        recurringExpenseTemplateRepository.save(template);

        auditLogService.log(
                AuditAction.DELETED,
                "RECURRING_EXPENSE_TEMPLATE",
                template.getId(),
                template.getName(),
                null
        );
    }

    @Override
    @Transactional
    public RecurringExpenseTemplateResponseDto pause(Long id) {
        RecurringExpenseTemplate template = findOrThrow(id);

        if (template.getStatus() != RecurringExpenseStatus.ACTIVE) {
            throw new BusinessRuleException("Only an ACTIVE template can be paused");
        }

        template.setStatus(RecurringExpenseStatus.PAUSED);
        return toResponse(recurringExpenseTemplateRepository.save(template));
    }

    @Override
    @Transactional
    public RecurringExpenseTemplateResponseDto resume(Long id) {
        RecurringExpenseTemplate template = findOrThrow(id);

        if (template.getStatus() != RecurringExpenseStatus.PAUSED) {
            throw new BusinessRuleException("Only a PAUSED template can be resumed");
        }

        // Fast-forward past any missed occurrences while it was paused, so resuming
        // doesn't suddenly generate a burst of backlogged expenses.
        LocalDate next = template.getNextRunDate();
        while (next.isBefore(LocalDate.now())) {
            next = advance(next, template.getFrequency());
        }
        template.setNextRunDate(next);

        if (template.getEndDate() != null && next.isAfter(template.getEndDate())) {
            template.setStatus(RecurringExpenseStatus.ENDED);
        } else {
            template.setStatus(RecurringExpenseStatus.ACTIVE);
        }

        return toResponse(recurringExpenseTemplateRepository.save(template));
    }

    @Override
    public RecurringExpenseTemplateResponseDto runNow(Long id) {
        return self.generateSingleTemplate(id);
    }

    @Override
    @Transactional
    public RecurringExpenseTemplateResponseDto generateSingleTemplate(Long id) {
        RecurringExpenseTemplate template = findOrThrow(id);

        if (template.getStatus() != RecurringExpenseStatus.ACTIVE) {
            throw new BusinessRuleException("Only an ACTIVE template can generate an expense");
        }

        LocalDate expenseDate = template.getNextRunDate();

        ExpenseRequestDto request = new ExpenseRequestDto();
        request.setExpenseDate(expenseDate);
        request.setExpenseAccountId(template.getExpenseAccount().getId());
        request.setPaidImmediately(template.getPaidImmediately());
        request.setPaymentAccountId(
                template.getPaymentAccount() != null ? template.getPaymentAccount().getId() : null);
        request.setPartyId(template.getParty() != null ? template.getParty().getId() : null);
        request.setAmount(template.getAmount());
        request.setReferenceNumber(template.getReferenceNumber());
        request.setNotes("Auto-generated from recurring template: " + template.getName());

        ExpenseResponseDto created = expenseService.createFromRecurringTemplate(request, template.getId());

        template.setLastGeneratedDate(expenseDate);
        template.setLastGeneratedExpenseId(created.getId());
        template.setLastRunError(null);

        LocalDate next = advance(template.getNextRunDate(), template.getFrequency());
        template.setNextRunDate(next);

        if (template.getEndDate() != null && next.isAfter(template.getEndDate())) {
            template.setStatus(RecurringExpenseStatus.ENDED);
        }

        RecurringExpenseTemplate saved = recurringExpenseTemplateRepository.save(template);

        auditLogService.log(
                AuditAction.CREATED,
                "RECURRING_EXPENSE_TEMPLATE",
                saved.getId(),
                null,
                "Generated expense " + created.getExpenseNumber() + " from template " + saved.getName()
        );

        return toResponse(saved);
    }

    @Override
    public void generateDueExpenses() {
        List<RecurringExpenseTemplate> due = recurringExpenseTemplateRepository
                .findByStatusAndNextRunDateLessThanEqualAndDeletedAtIsNull(
                        RecurringExpenseStatus.ACTIVE, LocalDate.now());

        for (RecurringExpenseTemplate template : due) {
            try {
                self.generateSingleTemplate(template.getId());
            } catch (Exception e) {
                markFailure(template.getId(), e.getMessage());
            }
        }
    }

    // _______ Private helpers __________

    private void markFailure(Long templateId, String errorMessage) {
        recurringExpenseTemplateRepository.findById(templateId).ifPresent(t -> {
            t.setLastRunError(errorMessage);
            recurringExpenseTemplateRepository.save(t);
        });
    }

    private LocalDate advance(LocalDate date, RecurringFrequency frequency) {
        return switch (frequency) {
            case WEEKLY -> date.plusWeeks(1);
            case MONTHLY -> date.plusMonths(1);
            case QUARTERLY -> date.plusMonths(3);
            case YEARLY -> date.plusYears(1);
        };
    }

    private Account getAccount(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));
    }

    private Party getParty(Long id) {
        return partyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Party not found: " + id));
    }

    private RecurringExpenseTemplate findOrThrow(Long id) {
        return recurringExpenseTemplateRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring expense template not found"));
    }

    private RecurringExpenseTemplateResponseDto toResponse(RecurringExpenseTemplate t) {
        return RecurringExpenseTemplateResponseDto.builder()
                .id(t.getId())
                .name(t.getName())
                .expenseAccountId(t.getExpenseAccount().getId())
                .expenseAccountName(t.getExpenseAccount().getName())
                .amount(t.getAmount())
                .paidImmediately(t.getPaidImmediately())
                .paymentAccountId(t.getPaymentAccount() != null ? t.getPaymentAccount().getId() : null)
                .paymentAccountName(t.getPaymentAccount() != null ? t.getPaymentAccount().getName() : null)
                .partyId(t.getParty() != null ? t.getParty().getId() : null)
                .partyName(t.getParty() != null ? t.getParty().getName() : null)
                .frequency(t.getFrequency())
                .startDate(t.getStartDate())
                .nextRunDate(t.getNextRunDate())
                .endDate(t.getEndDate())
                .lastGeneratedDate(t.getLastGeneratedDate())
                .lastGeneratedExpenseId(t.getLastGeneratedExpenseId())
                .status(t.getStatus())
                .referenceNumber(t.getReferenceNumber())
                .notes(t.getNotes())
                .lastRunError(t.getLastRunError())
                .build();
    }
}