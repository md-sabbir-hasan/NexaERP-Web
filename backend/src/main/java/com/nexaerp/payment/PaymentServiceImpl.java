package com.nexaerp.payment;

import com.nexaerp.account.Account;
import com.nexaerp.account.AccountRepository;
import com.nexaerp.accountingperiod.AccountingPeriodService;
import com.nexaerp.audit.AuditAction;
import com.nexaerp.audit.AuditLogService;
import com.nexaerp.banking.entity.BankAccount;
import com.nexaerp.banking.entity.BankTransaction;
import com.nexaerp.banking.enums.TransactionType;
import com.nexaerp.banking.repository.BankAccountRepository;
import com.nexaerp.banking.repository.BankTransactionRepository;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.expense.ExpenseRepository;
import com.nexaerp.invoice.Invoice;
import com.nexaerp.invoice.InvoiceRepository;
import com.nexaerp.invoice.InvoiceStatus;
import com.nexaerp.journal.*;
import com.nexaerp.party.Party;
import com.nexaerp.party.PartyRepository;
import com.nexaerp.party.PartyType;
import com.nexaerp.payment.dto.*;
import com.nexaerp.security.CurrentUserService;
import com.nexaerp.security.MakerCheckerService;
import com.nexaerp.settings.SettingKey;
import com.nexaerp.settings.SystemSettingsService;
import com.nexaerp.vendorbill.VendorBill;
import com.nexaerp.vendorbill.VendorBillRepository;
import com.nexaerp.vendorbill.VendorBillStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final PartyRepository partyRepository;
    private final AccountRepository accountRepository;
    private final InvoiceRepository invoiceRepository;
    private final VendorBillRepository vendorBillRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalLineRepository journalLineRepository;
    private final SystemSettingsService systemSettingsService;
    private final AuditLogService auditLogService;
    private final AccountingPeriodService accountingPeriodService;
    private final MakerCheckerService makerCheckerService;
    private final CurrentUserService currentUserService;
    private final BankAccountRepository bankAccountRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final ExpenseRepository expenseRepository;


    @Override
    @Transactional
    public PaymentResponseDto create(PaymentRequestDto request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Payment amount must be greater than zero");
        }

        Party party = partyRepository.findById(request.getPartyId())
                .orElseThrow(() -> new ResourceNotFoundException("Party not found"));

        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        validatePartyForPayment(request.getPaymentType(), party);
        validatePaymentAccount(account);

        // Build payment header
        Payment payment = new Payment();
        payment.setPaymentNumber(generatePaymentNumber());
        payment.setPaymentDate(request.getPaymentDate());
        payment.setPaymentType(request.getPaymentType());
        payment.setParty(party);
        payment.setAccount(account);
        payment.setAmount(request.getAmount());
        payment.setCurrencyCode(request.getCurrencyCode() != null ? request.getCurrencyCode() : "BDT");
        payment.setExchangeRate(BigDecimal.ONE);
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setTransactionRef(request.getTransactionRef());
        payment.setNotes(request.getNotes());
        payment.setStatus(PaymentStatus.DRAFT);

        Payment savedPayment = paymentRepository.save(payment);

        // Build allocation list - auto (FIFO) or manual (from request)
        List<PaymentAllocation> allocations;

        if (Boolean.TRUE.equals(request.getAutoAllocate())) {
            allocations = autoAllocateFifo(savedPayment, party.getId());
        } else {
            allocations = buildManualAllocations(request.getAllocations(), savedPayment);
        }

        paymentAllocationRepository.saveAll(allocations);

        // Calculate allocatedAmount and unallocatedAmount and store in payment
        BigDecimal totalAllocated = allocations.stream()
                .map(PaymentAllocation::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        savedPayment.setAllocatedAmount(totalAllocated);
        savedPayment.setUnallocatedAmount(savedPayment.getAmount().subtract(totalAllocated));
        paymentRepository.save(savedPayment);

        auditLogService.log(
                AuditAction.CREATED,
                "PAYMENT",
                savedPayment.getId(),
                null,
                savedPayment.getPaymentNumber()
        );

        return toResponse(savedPayment);
    }

    @Override
    public PaymentResponseDto getById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        return toResponse(payment);
    }

    @Override
    public List<PaymentResponseDto> getAll() {
        return paymentRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentResponseDto> getByParty(Long partyId) {
        return paymentRepository.findByPartyId(partyId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PartyOutstandingSummaryDto getOutstandingSummary(
            Long partyId,
            PaymentType paymentType
    ) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new ResourceNotFoundException("Party not found"));

        validatePartyForPayment(paymentType, party);

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal paidAmount = BigDecimal.ZERO;
        BigDecimal dueAmount = BigDecimal.ZERO;
        long documentCount = 0L;

        if (paymentType == PaymentType.RECEIPT) {

            List<Invoice> invoices = invoiceRepository.findByPartyId(partyId)
                    .stream()
                    .filter(invoice ->
                            invoice.getStatus() == InvoiceStatus.POSTED
                                    || invoice.getStatus() == InvoiceStatus.PARTIAL
                                    || invoice.getStatus() == InvoiceStatus.PAID
                    )
                    .toList();

            totalAmount = invoices.stream()
                    .map(Invoice::getGrandTotal)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            paidAmount = invoices.stream()
                    .map(Invoice::getPaidAmount)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            dueAmount = invoices.stream()
                    .map(Invoice::getDueAmount)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            documentCount = invoices.stream()
                    .filter(invoice ->
                            invoice.getDueAmount() != null
                                    && invoice.getDueAmount().compareTo(BigDecimal.ZERO) > 0
                    )
                    .count();

        } else if (paymentType == PaymentType.PAYMENT) {

            List<VendorBill> bills = vendorBillRepository.findByPartyId(partyId)
                    .stream()
                    .filter(bill ->
                            bill.getStatus() == VendorBillStatus.POSTED
                                    || bill.getStatus() == VendorBillStatus.PARTIAL
                                    || bill.getStatus() == VendorBillStatus.PAID
                    )
                    .toList();

            totalAmount = bills.stream()
                    .map(VendorBill::getNetPayable)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            paidAmount = bills.stream()
                    .map(VendorBill::getPaidAmount)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            dueAmount = bills.stream()
                    .map(VendorBill::getDueAmount)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            documentCount = bills.stream()
                    .filter(bill ->
                            bill.getDueAmount() != null
                                    && bill.getDueAmount().compareTo(BigDecimal.ZERO) > 0
                    )
                    .count();
        }

        return PartyOutstandingSummaryDto.builder()
                .partyId(party.getId())
                .partyName(party.getName())
                .totalAmount(totalAmount)
                .paidAmount(paidAmount)
                .dueAmount(dueAmount)
                .documentCount(documentCount)
                .build();
    }

    @Override
    @Transactional
    public PaymentResponseDto post(Long id) {

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Payment not found")
                );

        if (payment.getStatus() == PaymentStatus.POSTED) {
            throw new BusinessRuleException(
                    "Payment is already posted"
            );
        }

        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            throw new BusinessRuleException(
                    "Cannot post a cancelled payment"
            );
        }

        if (payment.getStatus() != PaymentStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Only DRAFT payments can be posted"
            );
        }

        if (payment.getParty() == null) {
            throw new BusinessRuleException(
                    "Payment party is required"
            );
        }

        if (payment.getAccount() == null) {
            throw new BusinessRuleException(
                    "Payment bank account is required"
            );
        }

        if (payment.getAmount() == null
                || payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException(
                    "Payment amount must be greater than zero"
            );
        }

        if (journalEntryRepository
                .findBySourceTypeAndSourceId(
                        JournalSourceType.PAYMENT,
                        payment.getId()
                )
                .isPresent()) {

            throw new BusinessRuleException(
                    "Journal entry already exists for this payment"
            );
        }

        /*
         * Creator cannot post their own payment.
         */
        makerCheckerService.validateChecker(
                payment.getCreatedBy(),
                "Payment"
        );

        /*
         * Payment date must belong to an OPEN accounting period.
         */
        accountingPeriodService.validatePostingDate(
                payment.getPaymentDate()
        );

        validateSufficientPaymentBalance(payment);

        createJournalEntry(payment);

        updateLinkedBankBalance(
                payment.getAccount(),
                payment.getAmount(),
                payment.getPaymentType(),
                false
        );

        createBankTransactionForPayment(payment);

        List<PaymentAllocation> allocations =
                paymentAllocationRepository.findByPaymentId(
                        payment.getId()
                );

        for (PaymentAllocation allocation : allocations) {
            applyAllocationToDocument(allocation);
        }

        payment.setStatus(PaymentStatus.POSTED);
        payment.setPostedAt(LocalDateTime.now());
        payment.setPostedBy(
                currentUserService.getCurrentUserId()
        );

        Payment saved = paymentRepository.save(payment);

        auditLogService.log(
                AuditAction.POSTED,
                "PAYMENT",
                saved.getId(),
                PaymentStatus.DRAFT.name(),
                PaymentStatus.POSTED.name()
        );

        return toResponse(saved);
    }

    @Override
    @Transactional
    public PaymentResponseDto cancel(Long id) {

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Payment not found")
                );

        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            throw new BusinessRuleException(
                    "Payment is already cancelled"
            );
        }

        PaymentStatus oldStatus = payment.getStatus();

        if (payment.getStatus() == PaymentStatus.POSTED) {

            LocalDate reversalDate = LocalDate.now();

            /*
             * Reversal journal uses today's date.
             */
            accountingPeriodService.validatePostingDate(
                    reversalDate
            );

            reverseJournalEntry(payment, reversalDate);

            updateLinkedBankBalance(
                    payment.getAccount(),
                    payment.getAmount(),
                    payment.getPaymentType(),
                    true
            );

            List<PaymentAllocation> allocations =
                    paymentAllocationRepository.findByPaymentId(
                            payment.getId()
                    );

            for (PaymentAllocation allocation : allocations) {
                undoAllocationFromDocument(allocation);
            }
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        payment.setCancelledAt(LocalDateTime.now());
        payment.setCancelledBy(
                currentUserService.getCurrentUserId()
        );

        Payment saved = paymentRepository.save(payment);

        auditLogService.log(
                AuditAction.CANCELLED,
                "PAYMENT",
                saved.getId(),
                oldStatus.name(),
                PaymentStatus.CANCELLED.name()
        );

        return toResponse(saved);
    }






                    //    -----Allocation Helper Method-------



//      FIFO auto allocation

    private List<PaymentAllocation> autoAllocateFifo(Payment payment, Long partyId) {

        BigDecimal remaining = payment.getAmount();
        List<PaymentAllocation> allocations = new java.util.ArrayList<>();

        if (payment.getPaymentType() == PaymentType.RECEIPT) {

            // Customer payment to allocate against Invoices
            List<Invoice> dueInvoices = invoiceRepository
                    .findByPartyIdAndDueAmountGreaterThanAndStatusNotOrderByDueDateAsc(
                            partyId, BigDecimal.ZERO, InvoiceStatus.CANCELLED);


            for (Invoice invoice : dueInvoices) {
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

                BigDecimal allocateAmount = remaining.min(invoice.getDueAmount());

                allocations.add(PaymentAllocation.builder()
                        .payment(payment)
                        .referenceType(PaymentReferenceType.INVOICE)
                        .referenceId(invoice.getId())
                        .allocatedAmount(allocateAmount)
                        .build());

                remaining = remaining.subtract(allocateAmount);
            }

        } else {

            // Vendor payment to allocate against Vendor Bills
            List<VendorBill> dueBills = vendorBillRepository
                    .findByPartyIdAndDueAmountGreaterThanAndStatusNotOrderByDueDateAsc(
                            partyId, BigDecimal.ZERO, VendorBillStatus.CANCELLED);

            
            for (VendorBill bill : dueBills) {
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

                BigDecimal allocateAmount = remaining.min(bill.getDueAmount());

                allocations.add(PaymentAllocation.builder()
                        .payment(payment)
                        .referenceType(PaymentReferenceType.VENDOR_BILL)
                        .referenceId(bill.getId())
                        .allocatedAmount(allocateAmount)
                        .build());

                remaining = remaining.subtract(allocateAmount);
            }
        }

        return allocations;
    }



// Apply manual allocations and validate allocation limits.

    private List<PaymentAllocation> buildManualAllocations(
            List<PaymentAllocationRequestDto> requestAllocations, Payment payment) {

        if (requestAllocations == null || requestAllocations.isEmpty()) {
            // No allocation provided — entire amount stays as advance
            return List.of();
        }

        BigDecimal totalRequested = requestAllocations.stream()
                .map(PaymentAllocationRequestDto::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalRequested.compareTo(payment.getAmount()) > 0) {
            throw new BusinessRuleException(
                    "Total allocated amount cannot exceed payment amount");
        }

        for (PaymentAllocationRequestDto dto : requestAllocations) {
            if (dto.getAllocatedAmount() == null || dto.getAllocatedAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRuleException("Allocation amount must be greater than zero");
            }

            if (dto.getReferenceType() == PaymentReferenceType.INVOICE) {
                if (payment.getPaymentType() != PaymentType.RECEIPT) {
                    throw new BusinessRuleException("Invoice allocation is only allowed for RECEIPT payments");
                }
                Invoice invoice = invoiceRepository.findById(dto.getReferenceId())
                        .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
                if (!invoice.getParty().getId().equals(payment.getParty().getId())) {
                    throw new BusinessRuleException("Allocated invoice does not belong to the selected party");
                }
                if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
                    throw new BusinessRuleException("Cannot allocate payment to a cancelled invoice");
                }
                if (invoice.getDueAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessRuleException("Invoice due amount must be greater than zero");
                }
                if (dto.getAllocatedAmount().compareTo(invoice.getDueAmount()) > 0) {
                    throw new BusinessRuleException("Allocation amount cannot exceed the remaining due amount of the invoice");
                }
            } else if (dto.getReferenceType() == PaymentReferenceType.VENDOR_BILL) {
                if (payment.getPaymentType() != PaymentType.PAYMENT) {
                    throw new BusinessRuleException("Vendor Bill allocation is only allowed for PAYMENT payments");
                }
                VendorBill bill = vendorBillRepository.findById(dto.getReferenceId())
                        .orElseThrow(() -> new ResourceNotFoundException("Vendor bill not found"));
                if (!bill.getParty().getId().equals(payment.getParty().getId())) {
                    throw new BusinessRuleException("Allocated vendor bill does not belong to the selected party");
                }
                if (bill.getStatus() == VendorBillStatus.CANCELLED) {
                    throw new BusinessRuleException("Cannot allocate payment to a cancelled vendor bill");
                }
                if (bill.getDueAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessRuleException("Vendor bill due amount must be greater than zero");
                }
                if (dto.getAllocatedAmount().compareTo(bill.getDueAmount()) > 0) {
                    throw new BusinessRuleException("Allocation amount cannot exceed the remaining due amount of the vendor bill");
                }
            }

            else if (dto.getReferenceType() == PaymentReferenceType.EXPENSE) {
                if (payment.getPaymentType() != PaymentType.PAYMENT) {
                    throw new BusinessRuleException("Expense allocation is only allowed for PAYMENT payments");
                }
                com.nexaerp.expense.Expense exp = expenseRepository.findById(dto.getReferenceId())
                        .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
                if (exp.getParty() == null || !exp.getParty().getId().equals(payment.getParty().getId())) {
                    throw new BusinessRuleException("Allocated expense does not belong to the selected party");
                }
                if (exp.getStatus() == com.nexaerp.expense.ExpenseStatus.CANCELLED) {
                    throw new BusinessRuleException("Cannot allocate payment to a cancelled expense");
                }
                if (exp.getDueAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessRuleException("Expense due amount must be greater than zero");
                }
                if (dto.getAllocatedAmount().compareTo(exp.getDueAmount()) > 0) {
                    throw new BusinessRuleException("Allocation amount cannot exceed the remaining due amount of the expense");
                }
                }


            else {
                throw new BusinessRuleException("Invalid allocation reference type");
            }
        }

        return requestAllocations.stream()
                .map(dto -> PaymentAllocation.builder()
                        .payment(payment)
                        .referenceType(dto.getReferenceType())
                        .referenceId(dto.getReferenceId())
                        .allocatedAmount(dto.getAllocatedAmount())
                        .build())
                .collect(Collectors.toList());
    }


// Apply allocation and update payment status on an Invoice/VendorBill.

    private void applyAllocationToDocument(PaymentAllocation allocation) {

        if (allocation.getReferenceType() == PaymentReferenceType.INVOICE) {

            Invoice invoice = invoiceRepository.findById(allocation.getReferenceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

            invoice.setPaidAmount(invoice.getPaidAmount().add(allocation.getAllocatedAmount()));
            invoice.setDueAmount(invoice.getGrandTotal().subtract(invoice.getPaidAmount()));

            invoice.setStatus(invoice.getDueAmount().compareTo(BigDecimal.ZERO) <= 0
                    ? InvoiceStatus.PAID
                    : InvoiceStatus.PARTIAL);

            invoiceRepository.save(invoice);

        } else if (allocation.getReferenceType() == PaymentReferenceType.VENDOR_BILL) {

            VendorBill bill = vendorBillRepository.findById(allocation.getReferenceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vendor bill not found"));

            bill.setPaidAmount(bill.getPaidAmount().add(allocation.getAllocatedAmount()));
            bill.setDueAmount(bill.getNetPayable().subtract(bill.getPaidAmount()));

            bill.setStatus(bill.getDueAmount().compareTo(BigDecimal.ZERO) <= 0
                    ? VendorBillStatus.PAID
                    : VendorBillStatus.PARTIAL);

            vendorBillRepository.save(bill);

        } else {

            com.nexaerp.expense.Expense exp = expenseRepository.findById(allocation.getReferenceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));

            exp.setPaidAmount(exp.getPaidAmount().add(allocation.getAllocatedAmount()));
            exp.setDueAmount(exp.getAmount().subtract(exp.getPaidAmount()));

            exp.setPaymentStatus(exp.getDueAmount().compareTo(BigDecimal.ZERO) <= 0
                    ? com.nexaerp.expense.ExpensePaymentStatus.PAID
                    : com.nexaerp.expense.ExpensePaymentStatus.PARTIAL);

            expenseRepository.save(exp);
        }
    }


//     what applyAllocationToDocument did — used when a posted payment is canceled.

    private void undoAllocationFromDocument(PaymentAllocation allocation) {

        if (allocation.getReferenceType() == PaymentReferenceType.INVOICE) {

            Invoice invoice = invoiceRepository.findById(allocation.getReferenceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

            invoice.setPaidAmount(invoice.getPaidAmount().subtract(allocation.getAllocatedAmount()));
            invoice.setDueAmount(invoice.getGrandTotal().subtract(invoice.getPaidAmount()));

            invoice.setStatus(invoice.getPaidAmount().compareTo(BigDecimal.ZERO) <= 0
                    ? InvoiceStatus.POSTED
                    : InvoiceStatus.PARTIAL);

            invoiceRepository.save(invoice);

        } else if (allocation.getReferenceType() == PaymentReferenceType.VENDOR_BILL) {

            VendorBill bill = vendorBillRepository.findById(allocation.getReferenceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vendor bill not found"));

            bill.setPaidAmount(bill.getPaidAmount().subtract(allocation.getAllocatedAmount()));
            bill.setDueAmount(bill.getNetPayable().subtract(bill.getPaidAmount()));

            bill.setStatus(bill.getPaidAmount().compareTo(BigDecimal.ZERO) <= 0
                    ? VendorBillStatus.POSTED
                    : VendorBillStatus.PARTIAL);

            vendorBillRepository.save(bill);

        } else {

            com.nexaerp.expense.Expense exp = expenseRepository.findById(allocation.getReferenceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));

            exp.setPaidAmount(exp.getPaidAmount().subtract(allocation.getAllocatedAmount()));
            exp.setDueAmount(exp.getAmount().subtract(exp.getPaidAmount()));

            exp.setPaymentStatus(exp.getPaidAmount().compareTo(BigDecimal.ZERO) <= 0
                    ? com.nexaerp.expense.ExpensePaymentStatus.UNPAID
                    : com.nexaerp.expense.ExpensePaymentStatus.PARTIAL);

            expenseRepository.save(exp);
        }
    }


                                   // ----Journal Entry Helper------


    private void createJournalEntry(Payment payment) {

        Account receivable = systemSettingsService.getAccount(
                SettingKey.DEFAULT_RECEIVABLE_ACCOUNT);
        Account payable = systemSettingsService.getAccount(
                SettingKey.DEFAULT_PAYABLE_ACCOUNT);

        JournalEntry entry = new JournalEntry();
        entry.setEntryNumber(generateJournalNumber());
        entry.setDate(payment.getPaymentDate());
        entry.setDescription("Payment - " + payment.getPaymentNumber());
        entry.setType(JournalEntryType.CASH);
        entry.setStatus(JournalStatus.POSTED);
        entry.setSourceType(JournalSourceType.PAYMENT);
        entry.setSourceId(payment.getId());
        entry.setTotalAmount(payment.getAmount());
        entry.setReferenceNumber(payment.getPaymentNumber());

        JournalEntry saved = journalEntryRepository.save(entry);

        if (payment.getPaymentType() == PaymentType.RECEIPT) {
            // Money coming in: Debit Cash/Bank, Credit Accounts Receivable
            saveLineAndUpdateBalance(saved, payment.getAccount(), payment.getAmount(), BigDecimal.ZERO);
            saveLineAndUpdateBalance(saved, receivable, BigDecimal.ZERO, payment.getAmount());
        } else {
            // Money going out: Debit Accounts Payable, Credit Cash/Bank
            saveLineAndUpdateBalance(saved, payable, payment.getAmount(), BigDecimal.ZERO);
            saveLineAndUpdateBalance(saved, payment.getAccount(), BigDecimal.ZERO, payment.getAmount());
        }
    }

    private void reverseJournalEntry(Payment payment, LocalDate reversalDate) {

        journalEntryRepository
                .findBySourceTypeAndSourceId(JournalSourceType.PAYMENT, payment.getId())
                .ifPresent(original -> {
                    if (original.getStatus() == JournalStatus.REVERSED) {
                        throw new BusinessRuleException("Journal entry is already reversed");
                    }

                    JournalEntry reversal = new JournalEntry();
                    reversal.setEntryNumber(generateJournalNumber());
                    reversal.setDate(reversalDate);
                    reversal.setDescription("Reversal - " + payment.getPaymentNumber());
                    reversal.setType(JournalEntryType.CASH);
                    reversal.setStatus(JournalStatus.POSTED);
                    reversal.setSourceType(JournalSourceType.PAYMENT);
                    reversal.setSourceId(payment.getId());
                    reversal.setTotalAmount(original.getTotalAmount());
                    reversal.setReversedFromId(original.getId());
                    reversal.setReferenceNumber("REV-" + original.getReferenceNumber());

                    JournalEntry savedReversal = journalEntryRepository.save(reversal);

                    List<JournalLine> originalLines =
                            journalLineRepository.findByJournalEntryId(original.getId());

                    originalLines.forEach(line -> {
                        saveLineAndUpdateBalance(savedReversal, line.getAccount(),
                                line.getCredit(), line.getDebit()); // swapped
                    });

                    original.setStatus(JournalStatus.REVERSED);
                    journalEntryRepository.save(original);
                });
    }

    private void saveLineAndUpdateBalance(JournalEntry entry, Account account,
                                          BigDecimal debit, BigDecimal credit) {
        JournalLine line = new JournalLine();
        line.setJournalEntry(entry);
        line.setAccount(account);
        line.setDebit(debit);
        line.setCredit(credit);
        journalLineRepository.save(line);

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



                                  // -------Number Generators--------

    private String generatePaymentNumber() {
        int year = Year.now().getValue();
        return paymentRepository.findTopByOrderByIdDesc()
                .map(last -> {
                    String[] parts = last.getPaymentNumber().split("-");
                    int next = Integer.parseInt(parts[2]) + 1;
                    return String.format("PAY-%d-%06d", year, next);
                })
                .orElse(String.format("PAY-%d-%06d", year, 1));
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

//    validation

    private void validatePartyForPayment(PaymentType paymentType, Party party) {
        if (!party.getIsActive()) {
            throw new BusinessRuleException("Selected party is inactive");
        }

        if (paymentType == PaymentType.RECEIPT) {
            if (!(party.getType() == PartyType.CUSTOMER || party.getType() == PartyType.BOTH)) {
                throw new BusinessRuleException("Receipt can only be created for Customer or Both type party");
            }
        }

        if (paymentType == PaymentType.PAYMENT) {
            if (!(party.getType() == PartyType.VENDOR || party.getType() == PartyType.BOTH)) {
                throw new BusinessRuleException("Payment can only be created for Vendor or Both type party");
            }
        }
    }

    private void validatePaymentAccount(Account account) {
        if (!account.getIsActive()) {
            throw new BusinessRuleException("Selected payment account is inactive");
        }

        if (account.getCurrentBalance() == null) {
            throw new BusinessRuleException("Selected payment account balance is invalid");
        }
    }


    // ---------bank balance update

    private void updateLinkedBankBalance(
            Account paymentAccount,
            BigDecimal amount,
            PaymentType paymentType,
            boolean reversal
    ) {
        BankAccount bankAccount = bankAccountRepository
                .findByCoaAccountId(paymentAccount.getId())
                .orElseThrow(() -> new BusinessRuleException(
                        "No bank account is linked with payment account: "
                                + paymentAccount.getCode()
                ));

        BigDecimal currentBalance = bankAccount.getCurrentBalance() != null
                ? bankAccount.getCurrentBalance()
                : BigDecimal.ZERO;

        BigDecimal adjustment;

        if (paymentType == PaymentType.RECEIPT) {
            adjustment = reversal ? amount.negate() : amount;
        } else {
            adjustment = reversal ? amount : amount.negate();
        }

        BigDecimal newBalance = currentBalance.add(adjustment);

        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException(
                    "Insufficient bank balance. Available: "
                            + currentBalance
                            + ", required: "
                            + amount
            );
        }

        bankAccount.setCurrentBalance(newBalance);
        bankAccountRepository.save(bankAccount);
    }

    // sufficient Balance validation

    private void validateSufficientPaymentBalance(Payment payment) {

        if (payment.getPaymentType() != PaymentType.PAYMENT) {
            return;
        }

        BankAccount bankAccount = bankAccountRepository
                .findByCoaAccountId(payment.getAccount().getId())
                .orElseThrow(() -> new BusinessRuleException(
                        "No bank account is linked with payment account: "
                                + payment.getAccount().getCode()
                ));

        BigDecimal availableBalance =
                bankAccount.getCurrentBalance() != null
                        ? bankAccount.getCurrentBalance()
                        : BigDecimal.ZERO;

        BigDecimal paymentAmount =
                payment.getAmount() != null
                        ? payment.getAmount()
                        : BigDecimal.ZERO;

        if (availableBalance.compareTo(paymentAmount) < 0) {
            throw new BusinessRuleException(
                    "Insufficient balance. Available: "
                            + availableBalance
                            + " BDT, required: "
                            + paymentAmount
                            + " BDT"
            );
        }
    }

    // -----------createBankTransactionForPayment----------
    private void createBankTransactionForPayment(Payment payment) {

        if (bankTransactionRepository
                .findByReferenceNumber(payment.getPaymentNumber())
                .isPresent()) {

            throw new BusinessRuleException(
                    "Bank transaction already exists for payment: "
                            + payment.getPaymentNumber()
            );
        }

        BankAccount bankAccount = bankAccountRepository
                .findByCoaAccountId(payment.getAccount().getId())
                .orElseThrow(() -> new BusinessRuleException(
                        "No bank account is linked with COA account: "
                                + payment.getAccount().getCode()
                ));

        BankTransaction transaction = new BankTransaction();

        transaction.setBankAccount(bankAccount);
        transaction.setTransactionDate(payment.getPaymentDate());
        transaction.setAmount(payment.getAmount());

        transaction.setTransactionType(
                payment.getPaymentType() == PaymentType.RECEIPT
                        ? TransactionType.CREDIT
                        : TransactionType.DEBIT
        );

        transaction.setReferenceNumber(payment.getPaymentNumber());

        transaction.setDescription(
                payment.getPaymentType() == PaymentType.RECEIPT
                        ? "Customer receipt - " + payment.getPaymentNumber()
                        : "Vendor payment - " + payment.getPaymentNumber()
        );

        transaction.setReconciled(false);
        transaction.setVoided(false);
        transaction.setTransactionNumber(
                "BT-" + payment.getPaymentNumber()
        );

        bankTransactionRepository.save(transaction);
    }


                                     // -------Mapper---------


    private PaymentResponseDto toResponse(Payment payment) {
        List<PaymentAllocation> allocations =
                paymentAllocationRepository.findByPaymentId(payment.getId());

        return PaymentResponseDto.builder()
                .id(payment.getId())
                .paymentNumber(payment.getPaymentNumber())
                .paymentDate(payment.getPaymentDate())
                .paymentType(payment.getPaymentType())
                .partyId(payment.getParty().getId())
                .partyName(payment.getParty().getName())
                .accountId(payment.getAccount().getId())
                .accountName(payment.getAccount().getName())
                .amount(payment.getAmount())
                .allocatedAmount(payment.getAllocatedAmount())
                .unallocatedAmount(payment.getUnallocatedAmount())
                .currencyCode(payment.getCurrencyCode())
                .exchangeRate(payment.getExchangeRate())
                .paymentMethod(payment.getPaymentMethod())
                .transactionRef(payment.getTransactionRef())
                .notes(payment.getNotes())
                .status(payment.getStatus())
                .postedAt(payment.getPostedAt())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .allocations(allocations.stream()
                        .map(this::toAllocationResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    private PaymentAllocationResponseDto toAllocationResponse(PaymentAllocation allocation) {
        return PaymentAllocationResponseDto.builder()
                .id(allocation.getId())
                .referenceType(allocation.getReferenceType())
                .referenceId(allocation.getReferenceId())
                .allocatedAmount(allocation.getAllocatedAmount())
                .createdAt(allocation.getCreatedAt())
                .build();
    }
}
