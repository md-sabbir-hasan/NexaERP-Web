package com.nexaerp.debitnote;

import com.nexaerp.account.Account;
import com.nexaerp.account.AccountRepository;
import com.nexaerp.accountingperiod.AccountingPeriodService;
import com.nexaerp.audit.AuditAction;
import com.nexaerp.audit.AuditLogService;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.debitnote.dto.DebitNoteItemRequestDto;
import com.nexaerp.debitnote.dto.DebitNoteItemResponseDto;
import com.nexaerp.debitnote.dto.DebitNoteRequestDto;
import com.nexaerp.debitnote.dto.DebitNoteResponseDto;
import com.nexaerp.journal.*;
import com.nexaerp.security.CurrentUserService;
import com.nexaerp.security.MakerCheckerService;
import com.nexaerp.settings.SettingKey;
import com.nexaerp.settings.SystemSettingsService;
import com.nexaerp.vendorbill.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DebitNoteServiceImpl implements DebitNoteService {

    private final DebitNoteRepository debitNoteRepository;
    private final DebitNoteItemRepository debitNoteItemRepository;
    private final VendorBillRepository vendorBillRepository;
    private final VendorBillItemRepository vendorBillItemRepository;
    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalLineRepository journalLineRepository;
    private final SystemSettingsService systemSettingsService;
    private final AccountingPeriodService accountingPeriodService;
    private final MakerCheckerService makerCheckerService;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public DebitNoteResponseDto create(DebitNoteRequestDto request) {
        VendorBill vendorBill = getEligibleVendorBill(request.getVendorBillId());
        DebitNote note = new DebitNote();
        note.setDebitNoteNumber(generateNumber());
        applyRequest(note, request, vendorBill);
        note.setStatus(DebitNoteStatus.DRAFT);
        DebitNote saved = debitNoteRepository.save(note);
        auditLogService.log(AuditAction.CREATED, "DEBIT_NOTE", saved.getId(), null, saved.getDebitNoteNumber());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public DebitNoteResponseDto update(Long id, DebitNoteRequestDto request) {
        DebitNote note = get(id);
        requireStatus(note, DebitNoteStatus.DRAFT, "Only DRAFT debit notes can be updated");
        VendorBill vendorBill = getEligibleVendorBill(request.getVendorBillId());
        note.getItems().clear();
        applyRequest(note, request, vendorBill);
        DebitNote saved = debitNoteRepository.save(note);
        auditLogService.log(AuditAction.UPDATED, "DEBIT_NOTE", saved.getId(), null, saved.getDebitNoteNumber());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DebitNoteResponseDto getById(Long id) { return toResponse(get(id)); }

    @Override
    @Transactional(readOnly = true)
    public List<DebitNoteResponseDto> getAll() {
        return debitNoteRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DebitNoteResponseDto> getByVendorBill(Long vendorBillId) {
        return debitNoteRepository.findByVendorBillIdOrderByIdDesc(vendorBillId).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public DebitNoteResponseDto approve(Long id) {
        DebitNote note = get(id);
        requireStatus(note, DebitNoteStatus.DRAFT, "Only DRAFT debit notes can be approved");
        makerCheckerService.validateChecker(note.getCreatedBy(), "Debit Note");
        revalidateItems(note);
        note.setStatus(DebitNoteStatus.APPROVED);
        note.setApprovedAt(LocalDateTime.now());
        note.setApprovedBy(currentUserService.getCurrentUserId());
        DebitNote saved = debitNoteRepository.save(note);
        auditLogService.log(AuditAction.APPROVED, "DEBIT_NOTE", saved.getId(), "DRAFT", "APPROVED");
        return toResponse(saved);
    }

    @Override
    @Transactional
    public DebitNoteResponseDto post(Long id) {
        DebitNote note = get(id);
        requireStatus(note, DebitNoteStatus.APPROVED, "Only APPROVED debit notes can be posted");
        makerCheckerService.validateChecker(note.getCreatedBy(), "Debit Note");
        accountingPeriodService.validatePostingDate(note.getPostingDate());
        revalidateItems(note);

        VendorBill vendorBill = note.getVendorBill();
        if (note.getNetAdjustment().compareTo(vendorBill.getDueAmount()) > 0) {
            throw new BusinessRuleException("Debit note total cannot exceed vendorBill due amount");
        }
        if (journalEntryRepository.findBySourceTypeAndSourceId(JournalSourceType.DEBIT_NOTE, note.getId()).isPresent()) {
            throw new BusinessRuleException("Journal entry already exists for this debit note");
        }

        createJournal(note);
        vendorBill.setDueAmount(vendorBill.getDueAmount().subtract(note.getNetAdjustment()));
        if (vendorBill.getDueAmount().compareTo(BigDecimal.ZERO) == 0) vendorBill.setStatus(VendorBillStatus.PAID);
        else if (vendorBill.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) vendorBill.setStatus(VendorBillStatus.PARTIAL);
        else vendorBill.setStatus(VendorBillStatus.POSTED);
        vendorBillRepository.save(vendorBill);

        note.setStatus(DebitNoteStatus.POSTED);
        note.setPostedAt(LocalDateTime.now());
        note.setPostedBy(currentUserService.getCurrentUserId());
        DebitNote saved = debitNoteRepository.save(note);
        auditLogService.log(AuditAction.POSTED, "DEBIT_NOTE", saved.getId(), "APPROVED", "POSTED");
        return toResponse(saved);
    }

    @Override
    @Transactional
    public DebitNoteResponseDto cancel(Long id, DebitNoteCancelledReason reason) {
        DebitNote note = get(id);
        if (note.getStatus() == DebitNoteStatus.CANCELLED) throw new BusinessRuleException("Debit note is already cancelled");
        if (reason == null) throw new BusinessRuleException("Cancellation reason is required");
        DebitNoteStatus old = note.getStatus();
        if (old == DebitNoteStatus.POSTED) {
            LocalDate reversalDate = LocalDate.now();
            accountingPeriodService.validatePostingDate(reversalDate);
            reverseJournal(note, reversalDate);
            VendorBill vendorBill = note.getVendorBill();
            vendorBill.setDueAmount(vendorBill.getDueAmount().add(note.getNetAdjustment()));
            vendorBill.setStatus(vendorBill.getPaidAmount().compareTo(BigDecimal.ZERO) > 0 ? VendorBillStatus.PARTIAL : VendorBillStatus.POSTED);
            vendorBillRepository.save(vendorBill);
        }
        note.setStatus(DebitNoteStatus.CANCELLED);
        note.setCancelledReason(reason);
        note.setCancelledAt(LocalDateTime.now());
        DebitNote saved = debitNoteRepository.save(note);
        auditLogService.log(AuditAction.CANCELLED, "DEBIT_NOTE", saved.getId(), old.name(), "CANCELLED");
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        DebitNote note = get(id);
        requireStatus(note, DebitNoteStatus.DRAFT, "Only DRAFT debit notes can be deleted");
        debitNoteRepository.delete(note);
        auditLogService.log(AuditAction.DELETED, "DEBIT_NOTE", id, note.getDebitNoteNumber(), null);
    }


    // private helper

    private void applyRequest(DebitNote note, DebitNoteRequestDto request, VendorBill vendorBill) {
        note.setDebitNoteDate(request.getDebitNoteDate());
        note.setPostingDate(request.getPostingDate() != null ? request.getPostingDate() : request.getDebitNoteDate());
        note.setVendorBill(vendorBill);
        note.setParty(vendorBill.getParty());
        note.setReason(request.getReason());
        note.setReference(request.getReference());
        note.setNotes(request.getNotes());
        List<DebitNoteItem> items = request.getItems().stream().map(dto -> buildItem(dto, note, vendorBill)).collect(Collectors.toList());
        note.getItems().addAll(items);
        calculateTotals(note);
        if (note.getGrandTotal().compareTo(BigDecimal.ZERO) <= 0)
            throw new BusinessRuleException("Debit note total must be greater than zero");
        if (note.getNetAdjustment().compareTo(vendorBill.getDueAmount()) > 0)
            throw new BusinessRuleException("Debit note total cannot exceed vendorBill due amount");
    }

    private DebitNoteItem buildItem(DebitNoteItemRequestDto dto, DebitNote note, VendorBill vendorBill) {
        VendorBillItem source = vendorBillItemRepository.findById(dto.getVendorBillItemId())
                .orElseThrow(() -> new ResourceNotFoundException("VendorBill item not found"));
        if (!source.getVendorBill().getId().equals(vendorBill.getId()))
            throw new BusinessRuleException("VendorBill item does not belong to selected vendorBill");
        BigDecimal already = debitNoteItemRepository.sumPostedQuantity(source.getId(), note.getId());
        BigDecimal remaining = source.getQuantity().subtract(already == null ? BigDecimal.ZERO : already);
        if (dto.getQuantity().compareTo(remaining) > 0)
            throw new BusinessRuleException("Credit quantity exceeds remaining quantity for item: " + source.getDescription());

        BigDecimal subTotal = dto.getQuantity().multiply(source.getUnitPrice()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discount = subTotal.multiply(source.getDiscountPercent()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal taxable = subTotal.subtract(discount);
        BigDecimal vat = taxable.multiply(source.getVatRate()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal tds = taxable.multiply(source.getTdsRate()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal lineTotal = taxable.add(vat);
        return DebitNoteItem.builder().debitNote(note).vendorBillItem(source).expenseAccount(source.getExpenseAccount()).description(source.getDescription())
                .quantity(dto.getQuantity()).unitPrice(source.getUnitPrice()).discountPercent(source.getDiscountPercent())
                .discountAmount(discount).vatRate(source.getVatRate()).vatAmount(vat).tdsRate(source.getTdsRate()).tdsAmount(tds)
                .subTotal(subTotal).lineTotal(lineTotal).netAdjustment(lineTotal.subtract(tds)).build();
    }

    private void calculateTotals(DebitNote note) {
        BigDecimal sub = note.getItems().stream().map(DebitNoteItem::getSubTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = note.getItems().stream().map(DebitNoteItem::getDiscountAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal vat = note.getItems().stream().map(DebitNoteItem::getVatAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tds = note.getItems().stream().map(DebitNoteItem::getTdsAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal grand = sub.subtract(discount).add(vat);
        note.setSubTotal(sub);
        note.setDiscountAmount(discount);
        note.setVatAmount(vat);
        note.setTdsAmount(tds);
        note.setGrandTotal(grand);
        note.setNetAdjustment(grand.subtract(tds));
    }

    private void revalidateItems(DebitNote note) {
        if (note.getItems() == null || note.getItems().isEmpty())
            throw new BusinessRuleException("Debit note must contain at least one item");
        for (DebitNoteItem item : note.getItems()) {
            BigDecimal already = debitNoteItemRepository.sumPostedQuantity(item.getVendorBillItem().getId(), note.getId());
            BigDecimal remaining = item.getVendorBillItem().getQuantity().subtract(already == null ? BigDecimal.ZERO : already);
            if (item.getQuantity().compareTo(remaining) > 0)
                throw new BusinessRuleException("Credit quantity exceeds remaining quantity for item: " + item.getDescription());
        }
    }

    private VendorBill getEligibleVendorBill(Long id) {
        VendorBill vendorBill = vendorBillRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("VendorBill not found"));
        if (vendorBill.getStatus() == VendorBillStatus.DRAFT || vendorBill.getStatus() == VendorBillStatus.CANCELLED)
            throw new BusinessRuleException("Debit note can only be created for a posted or partially paid vendorBill");
        if (vendorBill.getStatus() == VendorBillStatus.PAID)
            throw new BusinessRuleException("Paid vendorBill credit/refund is not supported in this version");
        if (vendorBill.getDueAmount() == null || vendorBill.getDueAmount().compareTo(BigDecimal.ZERO) <= 0)
            throw new BusinessRuleException("VendorBill has no remaining due amount");
        return vendorBill;
    }

    private void createJournal(DebitNote note) {
        Account payable = systemSettingsService.getAccount(SettingKey.DEFAULT_PAYABLE_ACCOUNT);
        Account purchaseReturn = systemSettingsService.getAccount(SettingKey.DEFAULT_PURCHASE_RETURN_ACCOUNT);
        Account inputVat = systemSettingsService.getAccount(SettingKey.DEFAULT_INPUT_VAT);
        Account tdsPayable = systemSettingsService.getAccount(SettingKey.DEFAULT_TDS_PAYABLE);
        JournalEntry entry = new JournalEntry();
        entry.setEntryNumber(generateJournalNumber());
        entry.setDate(note.getPostingDate());
        entry.setDescription("Debit Note - " + note.getDebitNoteNumber());
        entry.setType(JournalEntryType.PURCHASE);
        entry.setStatus(JournalStatus.POSTED);
        entry.setSourceType(JournalSourceType.DEBIT_NOTE);
        entry.setSourceId(note.getId());
        entry.setTotalAmount(note.getGrandTotal());
        entry.setReferenceNumber(note.getDebitNoteNumber());
        JournalEntry saved = journalEntryRepository.save(entry);
        BigDecimal purchaseReturnAmount = note.getSubTotal().subtract(note.getDiscountAmount());
        saveLine(saved, payable, note.getNetAdjustment(), BigDecimal.ZERO, "Accounts payable adjustment");
        saveLine(saved, purchaseReturn, BigDecimal.ZERO, purchaseReturnAmount, "Purchase return/adjustment");
        if (note.getVatAmount().compareTo(BigDecimal.ZERO) > 0)
            saveLine(saved, inputVat, BigDecimal.ZERO, note.getVatAmount(), "Input VAT adjustment");
        if (note.getTdsAmount().compareTo(BigDecimal.ZERO) > 0)
            saveLine(saved, tdsPayable, note.getTdsAmount(), BigDecimal.ZERO, "TDS payable adjustment");
    }

    private void reverseJournal(DebitNote note, LocalDate date) {
        JournalEntry original = journalEntryRepository.findBySourceTypeAndSourceId(JournalSourceType.DEBIT_NOTE, note.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Debit note journal entry not found"));
        if (original.getStatus() == JournalStatus.REVERSED)
            throw new BusinessRuleException("Debit note journal is already reversed");
        JournalEntry reversal = new JournalEntry();
        reversal.setEntryNumber(generateJournalNumber());
        reversal.setDate(date);
        reversal.setDescription("Reversal - " + note.getDebitNoteNumber());
        reversal.setType(JournalEntryType.PURCHASE);
        reversal.setStatus(JournalStatus.POSTED);
        reversal.setSourceType(JournalSourceType.DEBIT_NOTE);
        reversal.setSourceId(note.getId());
        reversal.setTotalAmount(original.getTotalAmount());
        reversal.setReversedFromId(original.getId());
        reversal.setReferenceNumber("REV-" + original.getReferenceNumber());
        JournalEntry saved = journalEntryRepository.save(reversal);
        for (JournalLine line : journalLineRepository.findByJournalEntryId(original.getId()))
            saveLine(saved, line.getAccount(), line.getCredit(), line.getDebit(), "Reversal: " + line.getDescription());
        original.setStatus(JournalStatus.REVERSED);
        journalEntryRepository.save(original);
    }

    private void saveLine(JournalEntry entry, Account account, BigDecimal debit, BigDecimal credit, String description) {
        JournalLine line = new JournalLine();
        line.setJournalEntry(entry);
        line.setAccount(account);
        line.setDebit(debit);
        line.setCredit(credit);
        line.setDescription(description);
        journalLineRepository.save(line);
        switch (account.getType()) {
            case ASSET, EXPENSE -> account.setCurrentBalance(account.getCurrentBalance().add(debit).subtract(credit));
            case LIABILITY, EQUITY, REVENUE ->
                    account.setCurrentBalance(account.getCurrentBalance().add(credit).subtract(debit));
        }
        accountRepository.save(account);
    }

    private DebitNote get(Long id) {
        return debitNoteRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Debit note not found"));
    }

    private void requireStatus(DebitNote n, DebitNoteStatus s, String message) {
        if (n.getStatus() != s) throw new BusinessRuleException(message);
    }

    private String generateNumber() {
        int year = Year.now().getValue();
        return debitNoteRepository.findTopByOrderByIdDesc().map(last -> {
            String[] p = last.getDebitNoteNumber().split("-");
            int next = Integer.parseInt(p[2]) + 1;
            return String.format("DN-%d-%06d", year, next);
        }).orElse(String.format("DN-%d-%06d", year, 1));
    }

    private String generateJournalNumber() {
        return journalEntryRepository.findTopByOrderByIdDesc().map(last -> {
            int next = Integer.parseInt(last.getEntryNumber().replace("JE-", "")) + 1;
            return String.format("JE-%04d", next);
        }).orElse("JE-0001");
    }

    private DebitNoteResponseDto toResponse(DebitNote n) {

        return DebitNoteResponseDto.builder()
                .id(n.getId())
                .debitNoteNumber(n.getDebitNoteNumber())
                .debitNoteDate(n.getDebitNoteDate())
                .postingDate(n.getPostingDate())

                .vendorBillId(
                        n.getVendorBill().getId()
                )

                .vendorBillNumber(
                        n.getVendorBill().getBillNumber()
                )

                .partyId(
                        n.getParty().getId()
                )

                .partyName(
                        n.getParty().getName()
                )

                .status(n.getStatus())
                .reason(n.getReason())
                .reference(n.getReference())
                .notes(n.getNotes())

                .subTotal(n.getSubTotal())
                .discountAmount(n.getDiscountAmount())
                .vatAmount(n.getVatAmount())
                .tdsAmount(n.getTdsAmount())
                .grandTotal(n.getGrandTotal())
                .netAdjustment(n.getNetAdjustment())

                .approvedAt(n.getApprovedAt())
                .postedAt(n.getPostedAt())
                .cancelledReason(n.getCancelledReason())
                .cancelledAt(n.getCancelledAt())

                .createdAt(n.getCreatedAt())
                .updatedAt(n.getUpdatedAt())

                .items(
                        n.getItems()
                                .stream()
                                .map(i ->
                                        DebitNoteItemResponseDto.builder()
                                                .id(i.getId())

                                                .vendorBillItemId(
                                                        i.getVendorBillItem().getId()
                                                )

                                                .expenseAccountId(
                                                        i.getExpenseAccount().getId()
                                                )

                                                .expenseAccountName(
                                                        i.getExpenseAccount().getName()
                                                )

                                                .description(i.getDescription())
                                                .quantity(i.getQuantity())
                                                .unitPrice(i.getUnitPrice())

                                                .discountPercent(
                                                        i.getDiscountPercent()
                                                )

                                                .discountAmount(
                                                        i.getDiscountAmount()
                                                )

                                                .vatRate(i.getVatRate())
                                                .vatAmount(i.getVatAmount())

                                                .tdsRate(i.getTdsRate())
                                                .tdsAmount(i.getTdsAmount())

                                                .subTotal(i.getSubTotal())
                                                .lineTotal(i.getLineTotal())

                                                .netAdjustment(
                                                        i.getNetAdjustment()
                                                )

                                                .build()
                                )
                                .toList()
                )

                .build();
    }
}
