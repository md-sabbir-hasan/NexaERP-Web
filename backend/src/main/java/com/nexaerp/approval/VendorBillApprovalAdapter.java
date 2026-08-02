package com.nexaerp.approval;

import com.nexaerp.audit.*;
import com.nexaerp.common.exception.*;
import com.nexaerp.party.PartyType;
import com.nexaerp.vendorbill.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class VendorBillApprovalAdapter implements ApprovalDocumentAdapter {
    private final ApprovalProperties properties;
    private final VendorBillRepository repository;
    private final AuditLogService auditLogService;

    @Override public ApprovalEntityType entityType() { return ApprovalEntityType.VENDOR_BILL; }
    @Override public boolean isEnabled() { return properties.isEnabled() && properties.getVendorBill().isEnabled(); }
    @Override public String requiredPermission() { return "APPROVE_VENDOR_BILL"; }
    @Override public String viewPermission() { return "VIEW_VENDOR_BILL"; }
    @Override public String displayName() { return "Vendor bill"; }
    @Override public Object lockDocument(Long id) { return repository.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Vendor bill not found")); }
    @Override public Object loadDocument(Long id) { return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Vendor bill not found")); }
    @Override public void validateForSubmission(Object value) {
        VendorBill bill = cast(value);
        if (bill.getStatus() != VendorBillStatus.DRAFT) throw rule("Only DRAFT vendor bills can be submitted");
        if (bill.getItems() == null || bill.getItems().isEmpty()) throw rule("Vendor bill must contain at least one item");
        if (bill.getParty() == null || bill.getParty().getType() == PartyType.CUSTOMER || !Boolean.TRUE.equals(bill.getParty().getIsActive())) throw rule("Vendor bill must reference an active vendor");
        if (bill.getNetPayable() == null || bill.getDueAmount() == null || bill.getNetPayable().compareTo(BigDecimal.ZERO) < 0) throw rule("Vendor bill totals are invalid");
        bill.getItems().forEach(item -> {
            if (item.getExpenseAccount() == null || !Boolean.TRUE.equals(item.getExpenseAccount().getIsActive())) throw rule("Vendor bill contains an inactive expense account");
            if (item.getQuantity() == null || item.getUnitPrice() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0 || item.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) throw rule("Vendor bill contains an invalid item");
        });
        BigDecimal subTotal = bill.getItems().stream().map(VendorBillItem::getSubTotal).reduce(BigDecimal.ZERO, this::addRequired);
        BigDecimal discount = bill.getItems().stream().map(VendorBillItem::getDiscountAmount).reduce(BigDecimal.ZERO, this::addRequired);
        BigDecimal vat = bill.getItems().stream().map(VendorBillItem::getVatAmount).reduce(BigDecimal.ZERO, this::addRequired);
        BigDecimal tds = bill.getItems().stream().map(VendorBillItem::getTdsAmount).reduce(BigDecimal.ZERO, this::addRequired);
        BigDecimal grandTotal = subTotal.subtract(discount).add(vat);
        BigDecimal netPayable = grandTotal.subtract(tds);
        if (!same(bill.getSubTotal(), subTotal) || !same(bill.getDiscountAmount(), discount)
                || !same(bill.getVatAmount(), vat) || !same(bill.getTdsAmount(), tds)
                || !same(bill.getGrandTotal(), grandTotal) || !same(bill.getNetPayable(), netPayable)
                || !same(bill.getDueAmount(), netPayable)) throw rule("Vendor bill persisted totals do not match its items");
    }
    @Override public void validatePending(Object value, ApprovalRequest request) {
        VendorBill bill = cast(value);
        if (bill.getStatus() != VendorBillStatus.DRAFT) throw rule("Vendor bill is no longer an eligible DRAFT");
        if (!Objects.equals(bill.getUpdatedAt(), request.getDocumentUpdatedAt())) throw rule("Vendor bill changed after submission; submit it again");
    }
    @Override public LocalDateTime approve(Object value, Long actorId) {
        VendorBill bill = cast(value);
        bill.setStatus(VendorBillStatus.APPROVED);
        bill.setApprovedAt(LocalDateTime.now());
        bill.setApprovedBy(actorId);
        repository.saveAndFlush(bill);
        auditLogService.log(AuditAction.APPROVED, "VENDOR_BILL", bill.getId(), VendorBillStatus.DRAFT.name(), VendorBillStatus.APPROVED.name());
        return bill.getUpdatedAt();
    }
    @Override public Long creatorId(Object document) { return cast(document).getCreatedBy(); }
    @Override public LocalDateTime updatedAt(Object document) { return cast(document).getUpdatedAt(); }
    @Override public String documentNumber(Object document) { return cast(document).getBillNumber(); }
    @Override public String documentTitle(Object document) { return cast(document).getParty() == null ? null : cast(document).getParty().getName(); }
    @Override public String documentUrl(Long id) { return "/vendor-bill/" + id; }
    private VendorBill cast(Object value) { return (VendorBill) value; }
    private BigDecimal addRequired(BigDecimal left, BigDecimal right) { if (right == null) throw rule("Vendor bill contains incomplete item totals"); return left.add(right); }
    private boolean same(BigDecimal left, BigDecimal right) { return left != null && left.compareTo(right) == 0; }
    private BusinessRuleException rule(String message) { return new BusinessRuleException(message); }
}
