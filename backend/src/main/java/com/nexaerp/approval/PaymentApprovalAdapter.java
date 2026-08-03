package com.nexaerp.approval;

import com.nexaerp.banking.entity.BankAccount;
import com.nexaerp.banking.repository.BankAccountRepository;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.expense.Expense;
import com.nexaerp.expense.ExpenseRepository;
import com.nexaerp.expense.ExpenseStatus;
import com.nexaerp.invoice.Invoice;
import com.nexaerp.invoice.InvoiceRepository;
import com.nexaerp.invoice.InvoiceStatus;
import com.nexaerp.party.PartyType;
import com.nexaerp.payment.*;
import com.nexaerp.vendorbill.VendorBill;
import com.nexaerp.vendorbill.VendorBillRepository;
import com.nexaerp.vendorbill.VendorBillStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class PaymentApprovalAdapter implements ApprovalDocumentAdapter {
    private final ApprovalProperties properties;
    private final PaymentRepository paymentRepository;
    private final PaymentAllocationRepository allocationRepository;
    private final BankAccountRepository bankAccountRepository;
    private final InvoiceRepository invoiceRepository;
    private final VendorBillRepository vendorBillRepository;
    private final ExpenseRepository expenseRepository;

    @Override public ApprovalEntityType entityType() { return ApprovalEntityType.PAYMENT; }
    @Override public boolean isEnabled() { return properties.isEnabled() && properties.getPayment().isEnabled(); }
    @Override public String requiredPermission() { return "APPROVE_PAYMENT"; }
    @Override public String viewPermission() { return "VIEW_PAYMENT"; }
    @Override public String displayName() { return "Payment"; }
    @Override public Object lockDocument(Long id) { return paymentRepository.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Payment not found")); }
    @Override public Object loadDocument(Long id) { return paymentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Payment not found")); }

    @Override
    public void validateForSubmission(Object value) {
        Payment payment = cast(value);
        if (payment.getStatus() != PaymentStatus.DRAFT) throw rule("Only DRAFT payments can be submitted");
        validateHeader(payment);

        var allocations = allocationRepository.findByPaymentId(payment.getId());
        BigDecimal total = allocations.stream().map(PaymentAllocation::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, this::addRequired);
        if (!same(payment.getAllocatedAmount(), total)
                || !same(payment.getUnallocatedAmount(), payment.getAmount().subtract(total))) {
            throw rule("Payment persisted allocation totals are invalid");
        }

        Map<String, BigDecimal> totalsByDocument = new HashMap<>();
        for (PaymentAllocation allocation : allocations) {
            if (allocation.getReferenceType() == null || allocation.getReferenceId() == null
                    || allocation.getAllocatedAmount() == null
                    || allocation.getAllocatedAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw rule("Payment contains an invalid allocation");
            }
            String key = allocation.getReferenceType() + ":" + allocation.getReferenceId();
            totalsByDocument.merge(key, allocation.getAllocatedAmount(), BigDecimal::add);
        }
        for (PaymentAllocation allocation : allocations) {
            String key = allocation.getReferenceType() + ":" + allocation.getReferenceId();
            validateAllocation(payment, allocation, totalsByDocument.remove(key));
        }
    }

    private void validateHeader(Payment payment) {
        if (payment.getParty() == null || !Boolean.TRUE.equals(payment.getParty().getIsActive()))
            throw rule("Payment must reference an active party");
        if (payment.getPaymentType() == PaymentType.RECEIPT
                && payment.getParty().getType() != PartyType.CUSTOMER && payment.getParty().getType() != PartyType.BOTH)
            throw rule("Receipt must reference an active customer");
        if (payment.getPaymentType() == PaymentType.PAYMENT
                && payment.getParty().getType() != PartyType.VENDOR && payment.getParty().getType() != PartyType.BOTH)
            throw rule("Payment must reference an active vendor");
        if (payment.getAccount() == null || !Boolean.TRUE.equals(payment.getAccount().getIsActive()))
            throw rule("Payment must reference an active payment account");
        BankAccount bank = bankAccountRepository.findByCoaAccountId(payment.getAccount().getId())
                .orElseThrow(() -> rule("Payment account must be linked to a bank account"));
        if (!Boolean.TRUE.equals(bank.getIsActive())) throw rule("Payment must reference an active bank account");
        if (payment.getAmount() == null || payment.getAmount().compareTo(BigDecimal.ZERO) <= 0)
            throw rule("Payment amount must be greater than zero");
        if (payment.getPaymentMethod() == null) throw rule("Payment method is required");
        if (payment.getExchangeRate() == null || payment.getExchangeRate().compareTo(BigDecimal.ZERO) <= 0)
            throw rule("Payment exchange rate must be greater than zero");
        if (payment.getCurrencyCode() == null || payment.getCurrencyCode().isBlank()) throw rule("Payment currency is required");
        if (bank.getCurrency() == null || !payment.getCurrencyCode().equalsIgnoreCase(bank.getCurrency()))
            throw rule("Payment currency must match the linked bank account currency");
    }

    private void validateAllocation(Payment payment, PaymentAllocation allocation, BigDecimal aggregateAmount) {
        if (aggregateAmount == null) return;
        switch (allocation.getReferenceType()) {
            case INVOICE -> validateInvoice(payment, allocation.getReferenceId(), aggregateAmount);
            case VENDOR_BILL -> validateVendorBill(payment, allocation.getReferenceId(), aggregateAmount);
            case EXPENSE -> validateExpense(payment, allocation.getReferenceId(), aggregateAmount);
        }
    }

    private void validateInvoice(Payment payment, Long id, BigDecimal amount) {
        if (payment.getPaymentType() != PaymentType.RECEIPT) throw rule("Invoice allocation requires a RECEIPT payment");
        Invoice invoice = invoiceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        if (invoice.getParty() == null || !Objects.equals(invoice.getParty().getId(), payment.getParty().getId())) throw rule("Allocated invoice does not belong to the payment party");
        if (invoice.getStatus() != InvoiceStatus.POSTED && invoice.getStatus() != InvoiceStatus.PARTIAL) throw rule("Allocated invoice must be POSTED or PARTIAL");
        validateDue(amount, invoice.getDueAmount(), "invoice");
        validateCurrency(payment.getCurrencyCode(), invoice.getCurrencyCode(), "invoice");
    }

    private void validateVendorBill(Payment payment, Long id, BigDecimal amount) {
        if (payment.getPaymentType() != PaymentType.PAYMENT) throw rule("Vendor bill allocation requires a PAYMENT payment");
        VendorBill bill = vendorBillRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Vendor bill not found"));
        if (bill.getParty() == null || !Objects.equals(bill.getParty().getId(), payment.getParty().getId())) throw rule("Allocated vendor bill does not belong to the payment party");
        if (bill.getStatus() != VendorBillStatus.POSTED && bill.getStatus() != VendorBillStatus.PARTIAL) throw rule("Allocated vendor bill must be POSTED or PARTIAL");
        validateDue(amount, bill.getDueAmount(), "vendor bill");
        validateCurrency(payment.getCurrencyCode(), bill.getCurrencyCode(), "vendor bill");
    }

    private void validateExpense(Payment payment, Long id, BigDecimal amount) {
        if (payment.getPaymentType() != PaymentType.PAYMENT) throw rule("Expense allocation requires a PAYMENT payment");
        Expense expense = expenseRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        if (expense.getParty() == null || !Objects.equals(expense.getParty().getId(), payment.getParty().getId())) throw rule("Allocated expense does not belong to the payment party");
        if (expense.getStatus() != ExpenseStatus.POSTED) throw rule("Allocated expense must be POSTED");
        validateDue(amount, expense.getDueAmount(), "expense");
    }

    private void validateDue(BigDecimal allocated, BigDecimal due, String label) {
        if (due == null || due.compareTo(BigDecimal.ZERO) <= 0) throw rule("Allocated " + label + " must have an outstanding amount");
        if (allocated.compareTo(due) > 0) throw rule("Allocation exceeds the outstanding " + label + " amount");
    }

    private void validateCurrency(String paymentCurrency, String documentCurrency, String label) {
        if (documentCurrency == null || !paymentCurrency.equalsIgnoreCase(documentCurrency))
            throw rule("Payment currency must match the allocated " + label + " currency");
    }

    @Override public void validatePending(Object value, ApprovalRequest request) {
        Payment payment = cast(value);
        if (payment.getStatus() != PaymentStatus.DRAFT) throw rule("Payment is no longer an eligible DRAFT");
        if (!Objects.equals(payment.getUpdatedAt(), request.getDocumentUpdatedAt())) throw rule("Payment changed after submission; submit it again");
    }
    @Override public LocalDateTime approve(Object document, Long actorId) { return cast(document).getUpdatedAt(); }
    @Override public Long creatorId(Object document) { return cast(document).getCreatedBy(); }
    @Override public LocalDateTime updatedAt(Object document) { return cast(document).getUpdatedAt(); }
    @Override public String documentNumber(Object document) { return cast(document).getPaymentNumber(); }
    @Override public String documentTitle(Object document) { return cast(document).getParty() == null ? null : cast(document).getParty().getName(); }
    @Override public String documentUrl(Long id) { return "/payment/" + id; }

    private Payment cast(Object value) { return (Payment) value; }
    private BigDecimal addRequired(BigDecimal left, BigDecimal right) { if (right == null) throw rule("Payment contains an incomplete allocation"); return left.add(right); }
    private boolean same(BigDecimal left, BigDecimal right) { return left != null && left.compareTo(right) == 0; }
    private BusinessRuleException rule(String message) { return new BusinessRuleException(message); }
}
