package com.nexaerp.invoice;

import com.nexaerp.account.Account;
import com.nexaerp.account.AccountRepository;
import com.nexaerp.account.AccountType;
import com.nexaerp.accountingperiod.AccountingPeriodService;
import com.nexaerp.approval.ApprovalService;
import com.nexaerp.fileupload.FileUploadService;
import com.nexaerp.audit.AuditLogService;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.currency.service.CurrencyService;
import com.nexaerp.currency.service.ExchangeRateService;
import com.nexaerp.journal.JournalEntry;
import com.nexaerp.journal.JournalEntryRepository;
import com.nexaerp.journal.JournalLine;
import com.nexaerp.journal.JournalLineRepository;
import com.nexaerp.journal.JournalSourceType;
import com.nexaerp.journal.JournalStatus;
import com.nexaerp.notification.NotificationModule;
import com.nexaerp.notification.NotificationPriority;
import com.nexaerp.notification.NotificationService;
import com.nexaerp.notification.NotificationType;
import com.nexaerp.party.Party;
import com.nexaerp.party.PartyRepository;
import com.nexaerp.party.PartyType;
import com.nexaerp.security.CurrentUserService;
import com.nexaerp.security.MakerCheckerService;
import com.nexaerp.settings.SettingKey;
import com.nexaerp.settings.SystemSettingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceNotificationTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private InvoiceItemRepository invoiceItemRepository;
    @Mock private PartyRepository partyRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private JournalEntryRepository journalEntryRepository;
    @Mock private JournalLineRepository journalLineRepository;
    @Mock private SystemSettingsService systemSettingsService;
    @Mock private AuditLogService auditLogService;
    @Mock private AccountingPeriodService accountingPeriodService;
    @Mock private MakerCheckerService makerCheckerService;
    @Mock private CurrentUserService currentUserService;
    @Mock private ExchangeRateService exchangeRateService;
    @Mock private CurrencyService currencyService;
    @Mock private NotificationService notificationService;
    @Mock private ApprovalService approvalService;
    @Mock private FileUploadService fileUploadService;

    @InjectMocks private InvoiceServiceImpl service;

    @Test
    void successfulPostingSchedulesExactNotification() {
        Invoice invoice = preparePostableInvoice();

        service.post(invoice.getId());

        assertEquals(InvoiceStatus.POSTED, invoice.getStatus());
        verify(notificationService).scheduleUniqueForUsersAfterCommit(
                Arrays.asList(55L, 99L),
                NotificationType.INVOICE_POSTED,
                NotificationPriority.MEDIUM,
                NotificationModule.INVOICE,
                "Invoice posted",
                "Invoice INV-2026-000001 was posted successfully.",
                "/invoice/1",
                "INVOICE",
                1L
        );
    }

    @Test
    void successfulPostingPassesSameCreatorAndActorSafely() {
        Invoice invoice = preparePostableInvoice();
        invoice.setCreatedBy(99L);

        service.post(invoice.getId());

        verify(notificationService).scheduleUniqueForUsersAfterCommit(
                Arrays.asList(99L, 99L),
                NotificationType.INVOICE_POSTED,
                NotificationPriority.MEDIUM,
                NotificationModule.INVOICE,
                "Invoice posted",
                "Invoice INV-2026-000001 was posted successfully.",
                "/invoice/1",
                "INVOICE",
                1L
        );
    }

    @Test
    void repeatedPostingSchedulesNothing() {
        Invoice invoice = invoice(InvoiceStatus.POSTED, BigDecimal.ZERO);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        assertThrows(BusinessRuleException.class, () -> service.post(1L));

        verifyNoNotification();
    }

    @Test
    void makerCheckerRejectionSchedulesNothing() {
        Invoice invoice = basicPostCandidate();
        doThrow(new BusinessRuleException("Creator cannot post their own Invoice"))
                .when(makerCheckerService).validateChecker(55L, "Invoice");

        assertThrows(BusinessRuleException.class, () -> service.post(invoice.getId()));

        verifyNoNotification();
    }

    @Test
    void closedPeriodRejectionSchedulesNothing() {
        Invoice invoice = basicPostCandidate();
        doThrow(new BusinessRuleException("Accounting period is closed"))
                .when(accountingPeriodService).validatePostingDate(invoice.getInvoiceDate());

        assertThrows(BusinessRuleException.class, () -> service.post(invoice.getId()));

        verifyNoNotification();
    }

    @Test
    void existingJournalRejectionSchedulesNothing() {
        Invoice invoice = invoice(InvoiceStatus.DRAFT, BigDecimal.ZERO);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(invoiceItemRepository.findByInvoiceId(1L)).thenReturn(List.of(item(invoice)));
        when(journalEntryRepository.findBySourceTypeAndSourceId(JournalSourceType.INVOICE, 1L))
                .thenReturn(Optional.of(new JournalEntry()));

        assertThrows(BusinessRuleException.class, () -> service.post(1L));

        verifyNoNotification();
    }

    @Test
    void journalCreationFailureSchedulesNothing() {
        Invoice invoice = basicPostCandidate();
        when(systemSettingsService.getAccount(SettingKey.DEFAULT_RECEIVABLE_ACCOUNT))
                .thenThrow(new BusinessRuleException("Missing receivable account"));

        assertThrows(BusinessRuleException.class, () -> service.post(invoice.getId()));

        assertEquals(InvoiceStatus.DRAFT, invoice.getStatus());
        verifyNoNotification();
    }

    @Test
    void draftCancellationSchedulesExactNotification() {
        Invoice invoice = invoice(InvoiceStatus.DRAFT, BigDecimal.ZERO);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(invoice)).thenReturn(invoice);
        when(invoiceItemRepository.findByInvoiceId(1L)).thenReturn(List.of(item(invoice)));
        when(currentUserService.getCurrentUserId()).thenReturn(99L);

        service.cancel(1L, CancelledReason.WRONG_ENTRY);

        assertEquals(InvoiceStatus.CANCELLED, invoice.getStatus());
        verifyCancelledNotification();
        verify(journalEntryRepository, never()).findBySourceTypeAndSourceId(any(), any());
    }

    @Test
    void postedCancellationReversesJournalAndSchedulesExactNotification() {
        Invoice invoice = preparePostedCancellation(JournalStatus.POSTED);
        when(currentUserService.getCurrentUserId()).thenReturn(99L);

        service.cancel(1L, CancelledReason.WRONG_ENTRY);

        assertEquals(InvoiceStatus.CANCELLED, invoice.getStatus());
        verifyCancelledNotification();
    }

    @Test
    void missingOriginalJournalPreventsCancellationAndNotification() {
        Invoice invoice = invoice(InvoiceStatus.POSTED, BigDecimal.ZERO);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(journalEntryRepository.findBySourceTypeAndSourceId(JournalSourceType.INVOICE, 1L))
                .thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class,
                () -> service.cancel(1L, CancelledReason.WRONG_ENTRY));

        assertEquals(InvoiceStatus.POSTED, invoice.getStatus());
        verify(invoiceRepository, never()).save(invoice);
        verifyNoNotification();
    }

    @Test
    void alreadyReversedJournalPreventsCancellationAndNotification() {
        Invoice invoice = preparePostedCancellation(JournalStatus.REVERSED);

        assertThrows(BusinessRuleException.class,
                () -> service.cancel(1L, CancelledReason.WRONG_ENTRY));

        assertEquals(InvoiceStatus.POSTED, invoice.getStatus());
        verifyNoNotification();
    }

    @Test
    void paidOrPartialInvoiceRejectionSchedulesNothing() {
        Invoice invoice = invoice(InvoiceStatus.PARTIAL, new BigDecimal("25.00"));
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        assertThrows(BusinessRuleException.class,
                () -> service.cancel(1L, CancelledReason.WRONG_ENTRY));

        verifyNoNotification();
    }

    @Test
    void closedReversalPeriodSchedulesNothing() {
        Invoice invoice = invoice(InvoiceStatus.POSTED, BigDecimal.ZERO);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        doThrow(new BusinessRuleException("Accounting period is closed"))
                .when(accountingPeriodService).validatePostingDate(any(LocalDate.class));

        assertThrows(BusinessRuleException.class,
                () -> service.cancel(1L, CancelledReason.WRONG_ENTRY));

        assertEquals(InvoiceStatus.POSTED, invoice.getStatus());
        verifyNoNotification();
    }

    @Test
    void repeatedCancellationSchedulesNothing() {
        Invoice invoice = invoice(InvoiceStatus.CANCELLED, BigDecimal.ZERO);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        assertThrows(BusinessRuleException.class,
                () -> service.cancel(1L, CancelledReason.WRONG_ENTRY));

        verifyNoNotification();
    }

    private Invoice preparePostableInvoice() {
        Invoice invoice = basicPostCandidate();
        Account receivable = account(1L, AccountType.ASSET);
        Account revenue = account(2L, AccountType.REVENUE);
        Account vat = account(3L, AccountType.LIABILITY);
        when(systemSettingsService.getAccount(SettingKey.DEFAULT_RECEIVABLE_ACCOUNT))
                .thenReturn(receivable);
        when(systemSettingsService.getAccount(SettingKey.DEFAULT_SALES_REVENUE))
                .thenReturn(revenue);
        when(systemSettingsService.getAccount(SettingKey.DEFAULT_VAT_PAYABLE))
                .thenReturn(vat);
        when(journalEntryRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(invocation -> {
            JournalEntry entry = invocation.getArgument(0);
            if (entry.getId() == null) entry.setId(100L);
            return entry;
        });
        when(currentUserService.getCurrentUserId()).thenReturn(99L);
        when(invoiceRepository.save(invoice)).thenReturn(invoice);
        return invoice;
    }

    private Invoice basicPostCandidate() {
        Invoice invoice = invoice(InvoiceStatus.DRAFT, BigDecimal.ZERO);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(invoiceItemRepository.findByInvoiceId(1L)).thenReturn(List.of(item(invoice)));
        when(journalEntryRepository.findBySourceTypeAndSourceId(JournalSourceType.INVOICE, 1L))
                .thenReturn(Optional.empty());
        return invoice;
    }

    private Invoice preparePostedCancellation(JournalStatus journalStatus) {
        Invoice invoice = invoice(InvoiceStatus.POSTED, BigDecimal.ZERO);
        JournalEntry original = JournalEntry.builder()
                .id(100L)
                .entryNumber("JE-0001")
                .referenceNumber(invoice.getInvoiceNumber())
                .status(journalStatus)
                .totalAmount(invoice.getGrandTotal())
                .build();
        Account receivable = account(1L, AccountType.ASSET);
        JournalLine line = new JournalLine();
        line.setAccount(receivable);
        line.setDebit(invoice.getGrandTotal());
        line.setCredit(BigDecimal.ZERO);
        line.setDescription("Invoice");

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(journalEntryRepository.findBySourceTypeAndSourceId(JournalSourceType.INVOICE, 1L))
                .thenReturn(Optional.of(original));
        if (journalStatus != JournalStatus.REVERSED) {
            when(journalEntryRepository.findTopByOrderByIdDesc()).thenReturn(Optional.of(original));
            when(journalEntryRepository.save(any(JournalEntry.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(journalLineRepository.findByJournalEntryId(100L)).thenReturn(List.of(line));
            when(invoiceRepository.save(invoice)).thenReturn(invoice);
            when(invoiceItemRepository.findByInvoiceId(1L)).thenReturn(List.of(item(invoice)));
        }
        return invoice;
    }

    private Invoice invoice(InvoiceStatus status, BigDecimal paidAmount) {
        Invoice invoice = Invoice.builder()
                .id(1L)
                .invoiceNumber("INV-2026-000001")
                .invoiceDate(LocalDate.of(2026, 7, 15))
                .dueDate(LocalDate.of(2026, 8, 14))
                .party(Party.builder().id(5L).name("Customer").type(PartyType.CUSTOMER).build())
                .status(status)
                .subTotal(new BigDecimal("100.00"))
                .discountAmount(BigDecimal.ZERO)
                .vatAmount(BigDecimal.ZERO)
                .grandTotal(new BigDecimal("100.00"))
                .paidAmount(paidAmount)
                .dueAmount(new BigDecimal("100.00").subtract(paidAmount))
                .build();
        invoice.setCreatedBy(55L);
        return invoice;
    }

    private InvoiceItem item(Invoice invoice) {
        return InvoiceItem.builder()
                .id(10L)
                .invoice(invoice)
                .description("Service")
                .quantity(BigDecimal.ONE)
                .unitPrice(new BigDecimal("100.00"))
                .discountPercent(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .vatRate(BigDecimal.ZERO)
                .vatAmount(BigDecimal.ZERO)
                .subTotal(new BigDecimal("100.00"))
                .lineTotal(new BigDecimal("100.00"))
                .build();
    }

    private Account account(Long id, AccountType type) {
        Account account = new Account();
        account.setId(id);
        account.setCode("A-" + id);
        account.setName("Account " + id);
        account.setType(type);
        account.setIsActive(true);
        account.setCurrentBalance(BigDecimal.ZERO);
        return account;
    }

    private void verifyCancelledNotification() {
        verify(notificationService).scheduleUniqueForUsersAfterCommit(
                Arrays.asList(55L, 99L),
                NotificationType.INVOICE_CANCELLED,
                NotificationPriority.HIGH,
                NotificationModule.INVOICE,
                "Invoice cancelled",
                "Invoice INV-2026-000001 was cancelled.",
                "/invoice/1",
                "INVOICE",
                1L
        );
    }

    private void verifyNoNotification() {
        verify(notificationService, never()).scheduleUniqueForUsersAfterCommit(
                any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
    }
}
