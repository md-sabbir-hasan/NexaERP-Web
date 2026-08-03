package com.nexaerp.approval;

import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.invoice.Invoice;
import com.nexaerp.invoice.InvoiceItem;
import com.nexaerp.invoice.InvoiceRepository;
import com.nexaerp.invoice.InvoiceStatus;
import com.nexaerp.party.PartyType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class InvoiceApprovalAdapter implements ApprovalDocumentAdapter {
    private final ApprovalProperties properties;
    private final InvoiceRepository repository;

    @Override public ApprovalEntityType entityType() { return ApprovalEntityType.INVOICE; }
    @Override public boolean isEnabled() { return properties.isEnabled() && properties.getInvoice().isEnabled(); }
    @Override public String requiredPermission() { return "APPROVE_INVOICE"; }
    @Override public String viewPermission() { return "VIEW_INVOICE"; }
    @Override public String displayName() { return "Invoice"; }
    @Override public Object lockDocument(Long id) { return repository.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Invoice not found")); }
    @Override public Object loadDocument(Long id) { return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invoice not found")); }

    @Override
    public void validateForSubmission(Object value) {
        Invoice invoice = cast(value);
        if (invoice.getStatus() != InvoiceStatus.DRAFT) throw rule("Only DRAFT invoices can be submitted");
        if (invoice.getItems() == null || invoice.getItems().isEmpty()) throw rule("Invoice must contain at least one item");
        if (invoice.getParty() == null || !Boolean.TRUE.equals(invoice.getParty().getIsActive())
                || (invoice.getParty().getType() != PartyType.CUSTOMER && invoice.getParty().getType() != PartyType.BOTH)) {
            throw rule("Invoice must reference an active customer");
        }
        BigDecimal subTotal = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal vat = BigDecimal.ZERO;
        for (InvoiceItem item : invoice.getItems()) {
            validateItem(item);
            subTotal = subTotal.add(item.getSubTotal());
            discount = discount.add(item.getDiscountAmount());
            vat = vat.add(item.getVatAmount());
        }
        BigDecimal grandTotal = subTotal.subtract(discount).add(vat);
        if (!same(invoice.getSubTotal(), subTotal) || !same(invoice.getDiscountAmount(), discount)
                || !same(invoice.getVatAmount(), vat) || !same(invoice.getGrandTotal(), grandTotal)
                || !same(invoice.getPaidAmount(), BigDecimal.ZERO) || !same(invoice.getDueAmount(), grandTotal)) {
            throw rule("Invoice persisted totals do not match its items");
        }
        if (invoice.getInvoiceDate() == null || invoice.getPaymentTerms() == null || invoice.getPaymentTerms() < 0
                || !Objects.equals(invoice.getDueDate(), invoice.getInvoiceDate().plusDays(invoice.getPaymentTerms()))) {
            throw rule("Invoice payment terms or due date are invalid");
        }
    }

    private void validateItem(InvoiceItem item) {
        if (item.getQuantity() == null || item.getUnitPrice() == null || item.getDiscountPercent() == null
                || item.getVatRate() == null || item.getSubTotal() == null || item.getDiscountAmount() == null
                || item.getVatAmount() == null || item.getLineTotal() == null
                || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0 || item.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw rule("Invoice contains an invalid item");
        }
        BigDecimal subTotal = item.getQuantity().multiply(item.getUnitPrice()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discount = subTotal.multiply(item.getDiscountPercent()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal taxable = subTotal.subtract(discount);
        BigDecimal vat = taxable.multiply(item.getVatRate()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        if (!same(item.getSubTotal(), subTotal) || !same(item.getDiscountAmount(), discount)
                || !same(item.getVatAmount(), vat) || !same(item.getLineTotal(), taxable.add(vat))) {
            throw rule("Invoice contains inconsistent item totals");
        }
    }

    @Override public void validatePending(Object value, ApprovalRequest request) {
        Invoice invoice = cast(value);
        if (invoice.getStatus() != InvoiceStatus.DRAFT) throw rule("Invoice is no longer an eligible DRAFT");
        if (!Objects.equals(invoice.getUpdatedAt(), request.getDocumentUpdatedAt())) throw rule("Invoice changed after submission; submit it again");
    }
    @Override public LocalDateTime approve(Object document, Long actorId) { return cast(document).getUpdatedAt(); }
    @Override public Long creatorId(Object document) { return cast(document).getCreatedBy(); }
    @Override public LocalDateTime updatedAt(Object document) { return cast(document).getUpdatedAt(); }
    @Override public String documentNumber(Object document) { return cast(document).getInvoiceNumber(); }
    @Override public String documentTitle(Object document) { return cast(document).getParty() == null ? null : cast(document).getParty().getName(); }
    @Override public String documentUrl(Long id) { return "/invoice/" + id; }

    private Invoice cast(Object value) { return (Invoice) value; }
    private boolean same(BigDecimal left, BigDecimal right) { return left != null && left.compareTo(right) == 0; }
    private BusinessRuleException rule(String message) { return new BusinessRuleException(message); }
}
