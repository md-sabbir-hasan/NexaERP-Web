package com.nexaerp.creditnote;

import com.nexaerp.account.Account;
import com.nexaerp.account.AccountRepository;
import com.nexaerp.accountingperiod.AccountingPeriodService;
import com.nexaerp.audit.AuditAction;
import com.nexaerp.audit.AuditLogService;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.creditnote.dto.*;
import com.nexaerp.invoice.*;
import com.nexaerp.journal.*;
import com.nexaerp.security.CurrentUserService;
import com.nexaerp.security.MakerCheckerService;
import com.nexaerp.settings.SettingKey;
import com.nexaerp.settings.SystemSettingsService;
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
public class CreditNoteServiceImpl implements CreditNoteService {
    private final CreditNoteRepository creditNoteRepository;
    private final CreditNoteItemRepository creditNoteItemRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
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
    public CreditNoteResponseDto create(CreditNoteRequestDto request) {
        Invoice invoice = getEligibleInvoice(request.getInvoiceId());
        CreditNote note = new CreditNote();
        note.setCreditNoteNumber(generateNumber());
        applyRequest(note, request, invoice);
        note.setStatus(CreditNoteStatus.DRAFT);
        CreditNote saved = creditNoteRepository.save(note);
        auditLogService.log(AuditAction.CREATED, "CREDIT_NOTE", saved.getId(), null, saved.getCreditNoteNumber());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public CreditNoteResponseDto update(Long id, CreditNoteRequestDto request) {
        CreditNote note = get(id);
        requireStatus(note, CreditNoteStatus.DRAFT, "Only DRAFT credit notes can be updated");
        Invoice invoice = getEligibleInvoice(request.getInvoiceId());
        note.getItems().clear();
        applyRequest(note, request, invoice);
        CreditNote saved = creditNoteRepository.save(note);
        auditLogService.log(AuditAction.UPDATED, "CREDIT_NOTE", saved.getId(), null, saved.getCreditNoteNumber());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CreditNoteResponseDto getById(Long id) { return toResponse(get(id)); }

    @Override
    @Transactional(readOnly = true)
    public List<CreditNoteResponseDto> getAll() {
        return creditNoteRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CreditNoteResponseDto> getByInvoice(Long invoiceId) {
        return creditNoteRepository.findByInvoiceIdOrderByIdDesc(invoiceId).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public CreditNoteResponseDto approve(Long id) {
        CreditNote note = get(id);
        requireStatus(note, CreditNoteStatus.DRAFT, "Only DRAFT credit notes can be approved");
        makerCheckerService.validateChecker(note.getCreatedBy(), "Credit Note");
        revalidateItems(note);
        note.setStatus(CreditNoteStatus.APPROVED);
        note.setApprovedAt(LocalDateTime.now());
        note.setApprovedBy(currentUserService.getCurrentUserId());
        CreditNote saved = creditNoteRepository.save(note);
        auditLogService.log(AuditAction.APPROVED, "CREDIT_NOTE", saved.getId(), "DRAFT", "APPROVED");
        return toResponse(saved);
    }

    @Override
    @Transactional
    public CreditNoteResponseDto post(Long id) {
        CreditNote note = get(id);
        requireStatus(note, CreditNoteStatus.APPROVED, "Only APPROVED credit notes can be posted");
        makerCheckerService.validateChecker(note.getCreatedBy(), "Credit Note");
        accountingPeriodService.validatePostingDate(note.getPostingDate());
        revalidateItems(note);

        Invoice invoice = note.getInvoice();
        if (note.getGrandTotal().compareTo(invoice.getDueAmount()) > 0) {
            throw new BusinessRuleException("Credit note total cannot exceed invoice due amount");
        }
        if (journalEntryRepository.findBySourceTypeAndSourceId(JournalSourceType.CREDIT_NOTE, note.getId()).isPresent()) {
            throw new BusinessRuleException("Journal entry already exists for this credit note");
        }

        createJournal(note);
        invoice.setDueAmount(invoice.getDueAmount().subtract(note.getGrandTotal()));
        if (invoice.getDueAmount().compareTo(BigDecimal.ZERO) == 0) invoice.setStatus(InvoiceStatus.PAID);
        else if (invoice.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) invoice.setStatus(InvoiceStatus.PARTIAL);
        else invoice.setStatus(InvoiceStatus.POSTED);
        invoiceRepository.save(invoice);

        note.setStatus(CreditNoteStatus.POSTED);
        note.setPostedAt(LocalDateTime.now());
        note.setPostedBy(currentUserService.getCurrentUserId());
        CreditNote saved = creditNoteRepository.save(note);
        auditLogService.log(AuditAction.POSTED, "CREDIT_NOTE", saved.getId(), "APPROVED", "POSTED");
        return toResponse(saved);
    }

    @Override
    @Transactional
    public CreditNoteResponseDto cancel(Long id, CreditNoteCancelledReason reason) {
        CreditNote note = get(id);
        if (note.getStatus() == CreditNoteStatus.CANCELLED) throw new BusinessRuleException("Credit note is already cancelled");
        if (reason == null) throw new BusinessRuleException("Cancellation reason is required");
        CreditNoteStatus old = note.getStatus();
        if (old == CreditNoteStatus.POSTED) {
            LocalDate reversalDate = LocalDate.now();
            accountingPeriodService.validatePostingDate(reversalDate);
            reverseJournal(note, reversalDate);
            Invoice invoice = note.getInvoice();
            invoice.setDueAmount(invoice.getDueAmount().add(note.getGrandTotal()));
            invoice.setStatus(invoice.getPaidAmount().compareTo(BigDecimal.ZERO) > 0 ? InvoiceStatus.PARTIAL : InvoiceStatus.POSTED);
            invoiceRepository.save(invoice);
        }
        note.setStatus(CreditNoteStatus.CANCELLED);
        note.setCancelledReason(reason);
        note.setCancelledAt(LocalDateTime.now());
        CreditNote saved = creditNoteRepository.save(note);
        auditLogService.log(AuditAction.CANCELLED, "CREDIT_NOTE", saved.getId(), old.name(), "CANCELLED");
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        CreditNote note = get(id);
        requireStatus(note, CreditNoteStatus.DRAFT, "Only DRAFT credit notes can be deleted");
        creditNoteRepository.delete(note);
        auditLogService.log(AuditAction.DELETED, "CREDIT_NOTE", id, note.getCreditNoteNumber(), null);
    }

    private void applyRequest(CreditNote note, CreditNoteRequestDto request, Invoice invoice) {
        note.setCreditNoteDate(request.getCreditNoteDate());
        note.setPostingDate(request.getPostingDate() != null ? request.getPostingDate() : request.getCreditNoteDate());
        note.setInvoice(invoice);
        note.setParty(invoice.getParty());
        note.setReason(request.getReason());
        note.setReference(request.getReference());
        note.setNotes(request.getNotes());
        List<CreditNoteItem> items = request.getItems().stream().map(dto -> buildItem(dto, note, invoice)).collect(Collectors.toList());
        note.getItems().addAll(items);
        calculateTotals(note);
        if (note.getGrandTotal().compareTo(BigDecimal.ZERO) <= 0) throw new BusinessRuleException("Credit note total must be greater than zero");
        if (note.getGrandTotal().compareTo(invoice.getDueAmount()) > 0) throw new BusinessRuleException("Credit note total cannot exceed invoice due amount");
    }

    private CreditNoteItem buildItem(CreditNoteItemRequestDto dto, CreditNote note, Invoice invoice) {
        InvoiceItem source = invoiceItemRepository.findById(dto.getInvoiceItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice item not found"));
        if (!source.getInvoice().getId().equals(invoice.getId())) throw new BusinessRuleException("Invoice item does not belong to selected invoice");
        BigDecimal already = creditNoteItemRepository.sumPostedQuantity(source.getId(), note.getId());
        BigDecimal remaining = source.getQuantity().subtract(already == null ? BigDecimal.ZERO : already);
        if (dto.getQuantity().compareTo(remaining) > 0) throw new BusinessRuleException("Credit quantity exceeds remaining quantity for item: " + source.getDescription());

        BigDecimal subTotal = dto.getQuantity().multiply(source.getUnitPrice()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discount = subTotal.multiply(source.getDiscountPercent()).divide(BigDecimal.valueOf(100),2,RoundingMode.HALF_UP);
        BigDecimal taxable = subTotal.subtract(discount);
        BigDecimal vat = taxable.multiply(source.getVatRate()).divide(BigDecimal.valueOf(100),2,RoundingMode.HALF_UP);
        return CreditNoteItem.builder().creditNote(note).invoiceItem(source).description(source.getDescription())
                .quantity(dto.getQuantity()).unitPrice(source.getUnitPrice()).discountPercent(source.getDiscountPercent())
                .discountAmount(discount).vatRate(source.getVatRate()).vatAmount(vat).subTotal(subTotal).lineTotal(taxable.add(vat)).build();
    }

    private void calculateTotals(CreditNote note) {
        BigDecimal sub = note.getItems().stream().map(CreditNoteItem::getSubTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = note.getItems().stream().map(CreditNoteItem::getDiscountAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal vat = note.getItems().stream().map(CreditNoteItem::getVatAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        note.setSubTotal(sub); note.setDiscountAmount(discount); note.setVatAmount(vat); note.setGrandTotal(sub.subtract(discount).add(vat));
    }

    private void revalidateItems(CreditNote note) {
        if (note.getItems() == null || note.getItems().isEmpty()) throw new BusinessRuleException("Credit note must contain at least one item");
        for (CreditNoteItem item : note.getItems()) {
            BigDecimal already = creditNoteItemRepository.sumPostedQuantity(item.getInvoiceItem().getId(), note.getId());
            BigDecimal remaining = item.getInvoiceItem().getQuantity().subtract(already == null ? BigDecimal.ZERO : already);
            if (item.getQuantity().compareTo(remaining) > 0) throw new BusinessRuleException("Credit quantity exceeds remaining quantity for item: " + item.getDescription());
        }
    }

    private Invoice getEligibleInvoice(Long id) {
        Invoice invoice = invoiceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        if (invoice.getStatus() == InvoiceStatus.DRAFT || invoice.getStatus() == InvoiceStatus.CANCELLED)
            throw new BusinessRuleException("Credit note can only be created for a posted or partially paid invoice");
        if (invoice.getStatus() == InvoiceStatus.PAID) throw new BusinessRuleException("Paid invoice credit/refund is not supported in this version");
        if (invoice.getDueAmount() == null || invoice.getDueAmount().compareTo(BigDecimal.ZERO) <= 0)
            throw new BusinessRuleException("Invoice has no remaining due amount");
        return invoice;
    }

    private void createJournal(CreditNote note) {
        Account receivable = systemSettingsService.getAccount(SettingKey.DEFAULT_RECEIVABLE_ACCOUNT);
        Account salesReturn = systemSettingsService.getAccount(SettingKey.DEFAULT_SALES_RETURN_ACCOUNT);
        Account vatPayable = systemSettingsService.getAccount(SettingKey.DEFAULT_VAT_PAYABLE);
        JournalEntry entry = new JournalEntry();
        entry.setEntryNumber(generateJournalNumber()); entry.setDate(note.getPostingDate());
        entry.setDescription("Credit Note - " + note.getCreditNoteNumber()); entry.setType(JournalEntryType.SALES);
        entry.setStatus(JournalStatus.POSTED); entry.setSourceType(JournalSourceType.CREDIT_NOTE); entry.setSourceId(note.getId());
        entry.setTotalAmount(note.getGrandTotal()); entry.setReferenceNumber(note.getCreditNoteNumber());
        JournalEntry saved = journalEntryRepository.save(entry);
        BigDecimal revenueAdjustment = note.getSubTotal().subtract(note.getDiscountAmount());
        saveLine(saved, salesReturn, revenueAdjustment, BigDecimal.ZERO, "Sales return/adjustment");
        if (note.getVatAmount().compareTo(BigDecimal.ZERO) > 0) saveLine(saved, vatPayable, note.getVatAmount(), BigDecimal.ZERO, "Output VAT adjustment");
        saveLine(saved, receivable, BigDecimal.ZERO, note.getGrandTotal(), "Accounts receivable adjustment");
    }

    private void reverseJournal(CreditNote note, LocalDate date) {
        JournalEntry original = journalEntryRepository.findBySourceTypeAndSourceId(JournalSourceType.CREDIT_NOTE, note.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Credit note journal entry not found"));
        if (original.getStatus() == JournalStatus.REVERSED) throw new BusinessRuleException("Credit note journal is already reversed");
        JournalEntry reversal = new JournalEntry();
        reversal.setEntryNumber(generateJournalNumber()); reversal.setDate(date); reversal.setDescription("Reversal - " + note.getCreditNoteNumber());
        reversal.setType(JournalEntryType.SALES); reversal.setStatus(JournalStatus.POSTED); reversal.setSourceType(JournalSourceType.CREDIT_NOTE);
        reversal.setSourceId(note.getId()); reversal.setTotalAmount(original.getTotalAmount()); reversal.setReversedFromId(original.getId());
        reversal.setReferenceNumber("REV-" + original.getReferenceNumber());
        JournalEntry saved = journalEntryRepository.save(reversal);
        for (JournalLine line : journalLineRepository.findByJournalEntryId(original.getId()))
            saveLine(saved, line.getAccount(), line.getCredit(), line.getDebit(), "Reversal: " + line.getDescription());
        original.setStatus(JournalStatus.REVERSED); journalEntryRepository.save(original);
    }

    private void saveLine(JournalEntry entry, Account account, BigDecimal debit, BigDecimal credit, String description) {
        JournalLine line = new JournalLine(); line.setJournalEntry(entry); line.setAccount(account); line.setDebit(debit); line.setCredit(credit); line.setDescription(description);
        journalLineRepository.save(line);
        switch (account.getType()) {
            case ASSET, EXPENSE -> account.setCurrentBalance(account.getCurrentBalance().add(debit).subtract(credit));
            case LIABILITY, EQUITY, REVENUE -> account.setCurrentBalance(account.getCurrentBalance().add(credit).subtract(debit));
        }
        accountRepository.save(account);
    }

    private CreditNote get(Long id) { return creditNoteRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Credit note not found")); }
    private void requireStatus(CreditNote n, CreditNoteStatus s, String message) { if (n.getStatus()!=s) throw new BusinessRuleException(message); }
    private String generateNumber() {
        int year=Year.now().getValue();
        return creditNoteRepository.findTopByOrderByIdDesc().map(last -> {
            String[] p=last.getCreditNoteNumber().split("-"); int next=Integer.parseInt(p[2])+1;
            return String.format("CN-%d-%06d",year,next);
        }).orElse(String.format("CN-%d-%06d",year,1));
    }
    private String generateJournalNumber() {
        return journalEntryRepository.findTopByOrderByIdDesc().map(last -> {
            int next=Integer.parseInt(last.getEntryNumber().replace("JE-",""))+1; return String.format("JE-%04d",next);
        }).orElse("JE-0001");
    }
    private CreditNoteResponseDto toResponse(CreditNote n) {
        return CreditNoteResponseDto.builder().id(n.getId()).creditNoteNumber(n.getCreditNoteNumber()).creditNoteDate(n.getCreditNoteDate())
                .postingDate(n.getPostingDate()).invoiceId(n.getInvoice().getId()).invoiceNumber(n.getInvoice().getInvoiceNumber())
                .partyId(n.getParty().getId()).partyName(n.getParty().getName()).status(n.getStatus()).reason(n.getReason())
                .reference(n.getReference()).notes(n.getNotes()).subTotal(n.getSubTotal()).discountAmount(n.getDiscountAmount())
                .vatAmount(n.getVatAmount()).grandTotal(n.getGrandTotal()).approvedAt(n.getApprovedAt()).postedAt(n.getPostedAt())
                .cancelledAt(n.getCancelledAt()).cancelledReason(n.getCancelledReason()).createdAt(n.getCreatedAt()).updatedAt(n.getUpdatedAt())
                .items(n.getItems().stream().map(i -> CreditNoteItemResponseDto.builder().id(i.getId()).invoiceItemId(i.getInvoiceItem().getId())
                        .description(i.getDescription()).quantity(i.getQuantity()).unitPrice(i.getUnitPrice()).discountPercent(i.getDiscountPercent())
                        .discountAmount(i.getDiscountAmount()).vatRate(i.getVatRate()).vatAmount(i.getVatAmount()).subTotal(i.getSubTotal()).lineTotal(i.getLineTotal()).build()).toList()).build();
    }
}
