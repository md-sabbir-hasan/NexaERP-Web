package com.nexaerp.invoice;

import com.nexaerp.account.Account;
import com.nexaerp.account.AccountRepository;
import com.nexaerp.accountingperiod.AccountingPeriodService;
import com.nexaerp.audit.AuditAction;
import com.nexaerp.audit.AuditLogService;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.currency.service.CurrencyService;
import com.nexaerp.currency.service.ExchangeRateService;
import com.nexaerp.invoice.dto.InvoiceItemRequestDto;
import com.nexaerp.invoice.dto.InvoiceItemResponseDto;
import com.nexaerp.invoice.dto.InvoiceRequestDto;
import com.nexaerp.invoice.dto.InvoiceResponseDto;
import com.nexaerp.journal.*;
import com.nexaerp.party.Party;
import com.nexaerp.party.PartyRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService{

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final PartyRepository partyRepository;
    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalLineRepository journalLineRepository;
    private final SystemSettingsService systemSettingsService;
    private final AuditLogService auditLogService;
    private final AccountingPeriodService accountingPeriodService;
    private final MakerCheckerService makerCheckerService;
    private final CurrentUserService currentUserService;
    private final ExchangeRateService exchangeRateService;
    private final CurrencyService currencyService;

    @Override
    @Transactional
    public InvoiceResponseDto create(InvoiceRequestDto request) {

        Party party = partyRepository.findById(request.getPartyId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Party not found")
                );

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(generateInvoiceNumber());
        invoice.setInvoiceDate(request.getInvoiceDate());
        invoice.setParty(party);
        invoice.setStatus(InvoiceStatus.DRAFT);

        // BDT-only
        invoice.setCurrencyCode("BDT");
        invoice.setExchangeRate(BigDecimal.ONE);

        Integer paymentTerms = request.getPaymentTerms() != null
                ? request.getPaymentTerms()
                : party.getPaymentTerms();

        if (paymentTerms == null) {
            paymentTerms = 30;
        }

        invoice.setPaymentTerms(paymentTerms);
        invoice.setDueDate(
                request.getInvoiceDate().plusDays(paymentTerms)
        );

        invoice.setReference(request.getReference());
        invoice.setNotes(request.getNotes());
        invoice.setPdfGenerated(false);
        invoice.setPrintCount(0);

        invoice.setSubTotal(BigDecimal.ZERO);
        invoice.setDiscountAmount(BigDecimal.ZERO);
        invoice.setVatAmount(BigDecimal.ZERO);
        invoice.setGrandTotal(BigDecimal.ZERO);
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setDueAmount(BigDecimal.ZERO);

        Invoice saved = invoiceRepository.save(invoice);

        List<InvoiceItem> items = request.getItems()
                .stream()
                .map(itemDto -> buildItem(itemDto, saved))
                .collect(Collectors.toList());

        invoiceItemRepository.saveAll(items);

        // Calculates subtotal, discount, VAT, grand total and due amount
        calculateAndSaveTotals(saved, items);

        auditLogService.log(
                AuditAction.CREATED,
                "INVOICE",
                saved.getId(),
                null,
                saved.getInvoiceNumber()
        );

        return toResponse(saved);
    }

    @Override
    @Transactional
    public InvoiceResponseDto update(Long id, InvoiceRequestDto request) {

        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Invoice not found")
                );

        if (!InvoiceStatus.DRAFT.equals(invoice.getStatus())) {
            throw new BusinessRuleException(
                    "Only DRAFT invoices can be updated"
            );
        }

        Party party = partyRepository.findById(request.getPartyId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Party not found")
                );

        invoice.setParty(party);
        invoice.setInvoiceDate(request.getInvoiceDate());

        invoice.setPaymentTerms(
                request.getPaymentTerms() != null
                        ? request.getPaymentTerms()
                        : 30
        );

        invoice.setDueDate(
                request.getInvoiceDate()
                        .plusDays(invoice.getPaymentTerms())
        );

        invoice.setReference(request.getReference());
        invoice.setNotes(request.getNotes());

        /*
         * BDT-only invoice
         */
        invoice.setCurrencyCode("BDT");
        invoice.setExchangeRate(BigDecimal.ONE);

        /*
         * orphanRemoval-safe item replacement
         */
        if (invoice.getItems() == null) {
            invoice.setItems(new ArrayList<>());
        } else {
            invoice.getItems().clear();
        }

        List<InvoiceItem> newItems = request.getItems()
                .stream()
                .map(itemDto -> buildItem(itemDto, invoice))
                .collect(Collectors.toList());

        invoice.getItems().addAll(newItems);

        /*
         * Calculate invoice totals
         */
        calculateTotalsOnly(invoice, newItems);

        Invoice saved = invoiceRepository.save(invoice);

        auditLogService.log(
                AuditAction.UPDATED,
                "INVOICE",
                saved.getId(),
                null,
                saved.getInvoiceNumber()
        );

        return toResponse(saved);
    }

    @Override
    public InvoiceResponseDto getById(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        return toResponse(invoice);
    }

    @Override
    public List<InvoiceResponseDto> getAll() {
        return invoiceRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<InvoiceResponseDto> getByParty(Long partyId) {
        return invoiceRepository.findByPartyId(partyId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<InvoiceResponseDto> getByStatus(InvoiceStatus status) {
        return invoiceRepository.findByStatus(status)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public InvoiceResponseDto post(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (invoice.getStatus() == InvoiceStatus.POSTED) {
            throw new BusinessRuleException("Invoice is already posted");
        }

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot post a cancelled invoice");
        }

        if (!invoice.getStatus().equals(InvoiceStatus.DRAFT)) {
            throw new BusinessRuleException("Only DRAFT invoices can be posted");
        }

        List<InvoiceItem> items = invoiceItemRepository.findByInvoiceId(invoice.getId());
        if (items == null || items.isEmpty()) {
            throw new BusinessRuleException("Cannot post an invoice with zero items");
        }

        if (journalEntryRepository.findBySourceTypeAndSourceId(JournalSourceType.INVOICE, invoice.getId()).isPresent()) {
            throw new BusinessRuleException("Journal entry already exists for this invoice");
        }

        makerCheckerService.validateChecker(
                invoice.getCreatedBy(),
                "Invoice"
        );

        /*
         * Period lock validation must happen before:
         * - journal creation
         * - account balance update
         * - invoice status change
         */
        accountingPeriodService.validatePostingDate(
                invoice.getInvoiceDate()
        );

        //Make Auto Journal Entry
        createJournalEntry(invoice);

        invoice.setStatus(InvoiceStatus.POSTED);
        invoice.setPostedAt(LocalDateTime.now());
        invoice.setPostedBy(currentUserService.getCurrentUserId());

        Invoice saved = invoiceRepository.save(invoice);

        auditLogService.log(
                AuditAction.POSTED,
                "INVOICE",
                saved.getId(),
                "DRAFT",
                "POSTED"
        );

        return toResponse(saved);
    }

    @Override
    @Transactional
    public InvoiceResponseDto cancel(Long id, CancelledReason reason) {

        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Invoice not found")
                );

        if (InvoiceStatus.CANCELLED.equals(invoice.getStatus())) {
            throw new BusinessRuleException(
                    "Invoice is already cancelled"
            );
        }

        if (invoice.getPaidAmount() != null
                && invoice.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {

            throw new BusinessRuleException(
                    "Paid or partially paid invoices cannot be cancelled"
            );
        }

        if (reason == null) {
            throw new BusinessRuleException(
                    "Cancelled reason is required"
            );
        }

        InvoiceStatus oldStatus = invoice.getStatus();

        if (InvoiceStatus.POSTED.equals(invoice.getStatus())) {

            accountingPeriodService.validatePostingDate(
                    LocalDate.now()
            );

            reverseJournalEntry(invoice);
        }

        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoice.setCancelledReason(reason);
        invoice.setDueAmount(BigDecimal.ZERO);

        Invoice saved = invoiceRepository.save(invoice);

        auditLogService.log(
                AuditAction.CANCELLED,
                "INVOICE",
                saved.getId(),
                oldStatus.name(),
                InvoiceStatus.CANCELLED.name()
        );

        return toResponse(saved);
    }



                                  // ---Privet Helper--


    private InvoiceItem buildItem(InvoiceItemRequestDto dto, Invoice invoice) {

        BigDecimal subTotal = dto.getQuantity()
                .multiply(dto.getUnitPrice())
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal discountAmount = subTotal
                .multiply(dto.getDiscountPercent())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal afterDiscount = subTotal.subtract(discountAmount);

        BigDecimal vatAmount = afterDiscount
                .multiply(dto.getVatRate())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal lineTotal = afterDiscount.add(vatAmount);

        return InvoiceItem.builder()
                .invoice(invoice)
                .productId(dto.getProductId())
                .description(dto.getDescription())
                .quantity(dto.getQuantity())
                .unitPrice(dto.getUnitPrice())
                .discountPercent(dto.getDiscountPercent())
                .discountAmount(discountAmount)
                .vatRate(dto.getVatRate())
                .vatAmount(vatAmount)
                .subTotal(subTotal)
                .lineTotal(lineTotal)
                .build();
    }


    private void calculateAndSaveTotals(Invoice invoice, List<InvoiceItem> items) {

        BigDecimal subTotal = items.stream()
                .map(InvoiceItem::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountAmount = items.stream()
                .map(InvoiceItem::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal vatAmount = items.stream()
                .map(InvoiceItem::getVatAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal grandTotal = subTotal.subtract(discountAmount).add(vatAmount);

        invoice.setSubTotal(subTotal);
        invoice.setDiscountAmount(discountAmount);
        invoice.setVatAmount(vatAmount);
        invoice.setGrandTotal(grandTotal);
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setDueAmount(grandTotal);

        invoiceRepository.save(invoice);
    }


    private void createJournalEntry(Invoice invoice) {

        Account receivable = systemSettingsService.getAccount(
                SettingKey.DEFAULT_RECEIVABLE_ACCOUNT
        );

        Account salesRevenue = systemSettingsService.getAccount(
                SettingKey.DEFAULT_SALES_REVENUE
        );

        Account vatPayable = systemSettingsService.getAccount(
                SettingKey.DEFAULT_VAT_PAYABLE
        );

        BigDecimal revenueAmount = invoice.getSubTotal()
                .subtract(invoice.getDiscountAmount());

        // Journal Entry
        JournalEntry entry = new JournalEntry();
        entry.setEntryNumber(generateJournalNumber());
        entry.setDate(invoice.getInvoiceDate());
        entry.setDescription("Invoice - " + invoice.getInvoiceNumber());
        entry.setType(JournalEntryType.SALES);
        entry.setStatus(JournalStatus.POSTED);
        entry.setSourceType(JournalSourceType.INVOICE);
        entry.setSourceId(invoice.getId());
        entry.setTotalAmount(invoice.getGrandTotal());
        entry.setReferenceNumber(invoice.getInvoiceNumber());

        JournalEntry saved = journalEntryRepository.save(entry);

        // Debit — Accounts Receivable
        JournalLine receivableLine = new JournalLine();
        receivableLine.setJournalEntry(saved);
        receivableLine.setAccount(receivable);
        receivableLine.setDebit(invoice.getGrandTotal());
        receivableLine.setCredit(BigDecimal.ZERO);
        receivableLine.setDescription(
                "Invoice - " + invoice.getInvoiceNumber()
        );

        // Credit — Sales Revenue
        JournalLine revenueLine = new JournalLine();
        revenueLine.setJournalEntry(saved);
        revenueLine.setAccount(salesRevenue);
        revenueLine.setDebit(BigDecimal.ZERO);
        revenueLine.setCredit(revenueAmount);
        revenueLine.setDescription(
                "Sales Revenue - " + invoice.getInvoiceNumber()
        );

        journalLineRepository.save(receivableLine);
        journalLineRepository.save(revenueLine);

        // Credit — VAT Payable
        if (invoice.getVatAmount() != null
                && invoice.getVatAmount().compareTo(BigDecimal.ZERO) > 0) {

            JournalLine vatLine = new JournalLine();
            vatLine.setJournalEntry(saved);
            vatLine.setAccount(vatPayable);
            vatLine.setDebit(BigDecimal.ZERO);
            vatLine.setCredit(invoice.getVatAmount());
            vatLine.setDescription(
                    "VAT - " + invoice.getInvoiceNumber()
            );

            journalLineRepository.save(vatLine);
        }

        // Update account balances
        updateBalance(
                receivable,
                invoice.getGrandTotal(),
                BigDecimal.ZERO
        );

        updateBalance(
                salesRevenue,
                BigDecimal.ZERO,
                revenueAmount
        );

        if (invoice.getVatAmount() != null
                && invoice.getVatAmount().compareTo(BigDecimal.ZERO) > 0) {

            updateBalance(
                    vatPayable,
                    BigDecimal.ZERO,
                    invoice.getVatAmount()
            );
        }
    }


    private void reverseJournalEntry(Invoice invoice) {

        LocalDate reversalDate = LocalDate.now();

        journalEntryRepository
                .findBySourceTypeAndSourceId(
                        JournalSourceType.INVOICE,
                        invoice.getId()
                )
                .ifPresent(original -> {

                    if (original.getStatus() == JournalStatus.REVERSED) {
                        throw new BusinessRuleException(
                                "Journal entry is already reversed"
                        );
                    }

                    JournalEntry reversal = new JournalEntry();
                    reversal.setEntryNumber(generateJournalNumber());
                    reversal.setDate(reversalDate);
                    reversal.setDescription(
                            "Reversal - " + invoice.getInvoiceNumber()
                    );
                    reversal.setType(JournalEntryType.SALES);
                    reversal.setStatus(JournalStatus.POSTED);
                    reversal.setSourceType(JournalSourceType.INVOICE);
                    reversal.setSourceId(invoice.getId());
                    reversal.setTotalAmount(original.getTotalAmount());
                    reversal.setReversedFromId(original.getId());
                    reversal.setReferenceNumber(
                            "REV-" + original.getReferenceNumber()
                    );

                    JournalEntry savedReversal =
                            journalEntryRepository.save(reversal);

                    List<JournalLine> originalLines =
                            journalLineRepository
                                    .findByJournalEntryId(original.getId());

                    originalLines.forEach(line -> {
                        JournalLine reversalLine =
                                new JournalLine();

                        reversalLine.setJournalEntry(savedReversal);
                        reversalLine.setAccount(line.getAccount());
                        reversalLine.setDebit(line.getCredit());
                        reversalLine.setCredit(line.getDebit());
                        reversalLine.setDescription(
                                "Reversal: " + line.getDescription()
                        );

                        journalLineRepository.save(reversalLine);

                        updateBalance(
                                line.getAccount(),
                                line.getCredit(),
                                line.getDebit()
                        );
                    });

                    original.setStatus(JournalStatus.REVERSED);
                    journalEntryRepository.save(original);
                });
    }


    private void updateBalance(Account account, BigDecimal debit, BigDecimal credit) {
        switch (account.getType()) {
            case ASSET:
            case EXPENSE:
                account.setCurrentBalance(
                        account.getCurrentBalance().add(debit).subtract(credit));
                break;
            case LIABILITY:
            case EQUITY:
            case REVENUE:
                account.setCurrentBalance(
                        account.getCurrentBalance().add(credit).subtract(debit));
                break;
        }
        accountRepository.save(account);
    }


    private String generateInvoiceNumber() {
        int year = Year.now().getValue();
        return invoiceRepository.findTopByOrderByIdDesc()
                .map(last -> {
                    String[] parts = last.getInvoiceNumber().split("-");
                    int next = Integer.parseInt(parts[2]) + 1;
                    return String.format("INV-%d-%06d", year, next);
                })
                .orElse(String.format("INV-%d-%06d", year, 1));
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


    private void calculateTotalsOnly(Invoice invoice, List<InvoiceItem> items) {

        BigDecimal subTotal = items.stream()
                .map(InvoiceItem::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountAmount = items.stream()
                .map(InvoiceItem::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal vatAmount = items.stream()
                .map(InvoiceItem::getVatAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal grandTotal = subTotal.subtract(discountAmount).add(vatAmount);

        invoice.setSubTotal(subTotal);
        invoice.setDiscountAmount(discountAmount);
        invoice.setVatAmount(vatAmount);
        invoice.setGrandTotal(grandTotal);
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setDueAmount(grandTotal);
    }


                                                    //---Mapper---

    private InvoiceResponseDto toResponse(Invoice invoice) {
        List<InvoiceItem> items = invoiceItemRepository.findByInvoiceId(invoice.getId());

        return InvoiceResponseDto.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .invoiceDate(invoice.getInvoiceDate())
                .dueDate(invoice.getDueDate())
                .partyId(invoice.getParty().getId())
                .partyName(invoice.getParty().getName())
                .status(invoice.getStatus())
                .currencyCode(invoice.getCurrencyCode())
                .exchangeRate(invoice.getExchangeRate())
                .paymentTerms(invoice.getPaymentTerms())
                .reference(invoice.getReference())
                .notes(invoice.getNotes())
                .cancelledReason(invoice.getCancelledReason())
                .pdfGenerated(invoice.getPdfGenerated())
                .printCount(invoice.getPrintCount())
                .subTotal(invoice.getSubTotal())
                .discountAmount(invoice.getDiscountAmount())
                .vatAmount(invoice.getVatAmount())
                .grandTotal(invoice.getGrandTotal())
                .paidAmount(invoice.getPaidAmount())
                .dueAmount(invoice.getDueAmount())
                .postedAt(invoice.getPostedAt())
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .items(items.stream().map(this::toItemResponse).collect(Collectors.toList()))
                .build();
    }

    private InvoiceItemResponseDto toItemResponse(InvoiceItem item) {
        return InvoiceItemResponseDto.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .description(item.getDescription())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .discountPercent(item.getDiscountPercent())
                .discountAmount(item.getDiscountAmount())
                .vatRate(item.getVatRate())
                .vatAmount(item.getVatAmount())
                .subTotal(item.getSubTotal())
                .lineTotal(item.getLineTotal())
                .build();
    }
}
