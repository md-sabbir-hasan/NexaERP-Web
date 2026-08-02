package com.nexaerp.overdue;

import com.nexaerp.email.OverdueAlertEmailService;
import com.nexaerp.invoice.Invoice;
import com.nexaerp.invoice.InvoiceRepository;
import com.nexaerp.invoice.InvoiceStatus;
import com.nexaerp.party.Party;
import com.nexaerp.party.PartyType;
import com.nexaerp.user.User;
import com.nexaerp.user.UserRepository;
import com.nexaerp.user.UserStatus;
import com.nexaerp.vendorbill.VendorBill;
import com.nexaerp.vendorbill.VendorBillRepository;
import com.nexaerp.vendorbill.VendorBillStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OverdueReminderServiceImplTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private VendorBillRepository vendorBillRepository;
    @Mock private OverdueReminderDeliveryRepository deliveryRepository;
    @Mock private OverdueDeliveryClaimService claimService;
    @Mock private OverdueNotificationDeliveryService notificationDeliveryService;
    @Mock private OverdueAlertEmailService emailService;
    @Mock private UserRepository userRepository;

    private OverdueProperties properties;
    private OverdueReminderServiceImpl service;
    private final LocalDate businessDate = LocalDate.of(2026, 8, 10);

    @BeforeEach
    void setUp() {
        properties = new OverdueProperties();
        properties.setEnabled(true);
        properties.getEmail().setEnabled(false);
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-10T03:00:00Z"), ZoneId.of("Asia/Dhaka"));
        service = new OverdueReminderServiceImpl(
                invoiceRepository, vendorBillRepository, deliveryRepository, claimService,
                notificationDeliveryService, emailService, userRepository,
                new OverdueMessageFormatter(), new OverdueEligibility(), properties, clock);

        lenient().when(deliveryRepository.findRetryCandidates(any(), any(), anyInt(), any(Pageable.class)))
                .thenReturn(Page.empty());
        lenient().when(invoiceRepository.findByStatusInAndDueDateBeforeAndDueAmountGreaterThan(
                any(), any(), any(), any(Pageable.class))).thenReturn(Page.empty());
        lenient().when(vendorBillRepository.findByStatusInAndDueDateBeforeAndDueAmountGreaterThan(
                any(), any(), any(), any(Pageable.class))).thenReturn(Page.empty());
        lenient().when(userRepository.findById(55L)).thenReturn(Optional.of(activeUser()));
        lenient().when(notificationDeliveryService.deliver(anyLong(), any(), any())).thenReturn(true);
        lenient().when(claimService.claim(any(), anyLong(), anyInt(), any(), anyLong(), any(), any(), anyInt()))
                .thenAnswer(invocation -> Optional.of(delivery(
                        invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2),
                        invocation.getArgument(3), invocation.getArgument(4))));
    }

    @Test
    void discoveryUsesExactEligibilityAndConfigurablePaging() {
        properties.setBatchSize(25);

        service.processDueReminders();

        verify(invoiceRepository).findByStatusInAndDueDateBeforeAndDueAmountGreaterThan(
                eq(List.of(InvoiceStatus.POSTED, InvoiceStatus.PARTIAL)),
                eq(businessDate), eq(BigDecimal.ZERO), any(Pageable.class));
        verify(vendorBillRepository).findByStatusInAndDueDateBeforeAndDueAmountGreaterThan(
                eq(List.of(VendorBillStatus.POSTED, VendorBillStatus.PARTIAL)),
                eq(businessDate), eq(BigDecimal.ZERO), any(Pageable.class));
    }

    @Test
    void perDocumentFlagsControlDiscoveryIndependently() {
        properties.getInvoice().setEnabled(false);

        service.processDueReminders();

        verify(invoiceRepository, never()).findByStatusInAndDueDateBeforeAndDueAmountGreaterThan(
                any(), any(), any(), any(Pageable.class));
        verify(vendorBillRepository).findByStatusInAndDueDateBeforeAndDueAmountGreaterThan(
                any(), any(), any(), any(Pageable.class));
    }

    @Test
    void invoiceMilestonesAndDayNineCatchUpUseOnlyHighestReachedMilestone() {
        for (int days : List.of(1, 7, 15, 30)) {
            org.mockito.Mockito.clearInvocations(claimService);
            Invoice invoice = invoice(days, InvoiceStatus.POSTED, new BigDecimal("125.50"), 55L);
            mockInvoicePage(invoice);

            service.processDueReminders();

            verify(claimService).claim(eq(OverdueDocumentType.INVOICE), eq(1L), eq(days),
                    eq(OverdueReminderChannel.IN_APP), eq(55L), any(), any(), anyInt());
        }

        org.mockito.Mockito.clearInvocations(claimService);
        Invoice dayNine = invoice(9, InvoiceStatus.PARTIAL, new BigDecimal("50.00"), 55L);
        mockInvoicePage(dayNine);
        service.processDueReminders();
        verify(claimService).claim(eq(OverdueDocumentType.INVOICE), eq(1L), eq(7),
                eq(OverdueReminderChannel.IN_APP), eq(55L), any(), any(), anyInt());
        verify(claimService, never()).claim(any(), anyLong(), eq(1), any(), anyLong(), any(), any(), anyInt());
    }

    @Test
    void vendorBillPostedAndPartialAreDeliveredWithExactActualDaysMessage() {
        VendorBill bill = bill(15, VendorBillStatus.PARTIAL, new BigDecimal("200.00"), 55L);
        mockBillPage(bill);

        service.processDueReminders();

        ArgumentCaptor<OverdueDocumentSnapshot> document =
                ArgumentCaptor.forClass(OverdueDocumentSnapshot.class);
        verify(notificationDeliveryService).deliver(
                eq(55L), document.capture(),
                eq("Vendor bill BILL-001 is 15 days overdue with BDT 200.00 outstanding."));
        assertEquals("/vendor-bill/2", document.getValue().route());
    }

    @Test
    void dueTodayFutureAndIneligibleOrSettledDocumentsDoNotDeliver() {
        for (Invoice invoice : List.of(
                invoice(0, InvoiceStatus.POSTED, BigDecimal.TEN, 55L),
                invoice(-1, InvoiceStatus.POSTED, BigDecimal.TEN, 55L),
                invoice(1, InvoiceStatus.DRAFT, BigDecimal.TEN, 55L),
                invoice(1, InvoiceStatus.PAID, BigDecimal.ZERO, 55L),
                invoice(1, InvoiceStatus.CANCELLED, BigDecimal.TEN, 55L))) {
            mockInvoicePage(invoice);
            service.processDueReminders();
        }

        VendorBill approved = bill(1, VendorBillStatus.APPROVED, BigDecimal.TEN, 55L);
        mockBillPage(approved);
        service.processDueReminders();

        verify(notificationDeliveryService, never()).deliver(anyLong(), any(), any());
    }

    @Test
    void nullCreatorAndNonActiveRecipientsAreSkipped() {
        Invoice noCreator = invoice(1, InvoiceStatus.POSTED, BigDecimal.TEN, null);
        mockInvoicePage(noCreator);
        service.processDueReminders();
        verify(claimService).markSkipped(anyLong(), eq("Document creator is unavailable or changed"));

        for (UserStatus status : List.of(
                UserStatus.INACTIVE, UserStatus.LOCKED, UserStatus.PENDING)) {
            org.mockito.Mockito.clearInvocations(claimService);
            Invoice invoice = invoice(1, InvoiceStatus.POSTED, BigDecimal.TEN, 55L);
            mockInvoicePage(invoice);
            when(userRepository.findById(55L)).thenReturn(Optional.of(User.builder()
                    .id(55L).name("Creator").email("creator@example.com").status(status).build()));
            service.processDueReminders();
            verify(claimService).markSkipped(anyLong(), eq("Recipient user is missing or not active"));
        }
    }

    @Test
    void channelsAreIndependentAndBlankEmailIsSkippedWithoutUsingPartyEmail() {
        properties.getEmail().setEnabled(true);
        Invoice invoice = invoice(7, InvoiceStatus.POSTED, BigDecimal.TEN, 55L);
        invoice.getParty().setEmail("customer@example.com");
        mockInvoicePage(invoice);
        when(userRepository.findById(55L)).thenReturn(Optional.of(User.builder()
                .id(55L).name("Creator").email(" ").status(UserStatus.ACTIVE).build()));

        service.processDueReminders();

        verify(claimService).claim(eq(OverdueDocumentType.INVOICE), eq(1L), eq(7),
                eq(OverdueReminderChannel.IN_APP), eq(55L), any(), any(), anyInt());
        verify(claimService).claim(eq(OverdueDocumentType.INVOICE), eq(1L), eq(7),
                eq(OverdueReminderChannel.EMAIL), eq(55L), any(), any(), anyInt());
        verify(notificationDeliveryService).deliver(eq(55L), any(), any());
        verify(emailService, never()).send(any(), any());
        verify(claimService).markSkipped(anyLong(), eq("Recipient email is blank"));
    }

    @Test
    void notificationFailureDoesNotBlockEmailOrLaterDocuments() {
        properties.getEmail().setEnabled(true);
        Invoice first = invoice(1, InvoiceStatus.POSTED, BigDecimal.TEN, 55L);
        Invoice second = invoice(1, InvoiceStatus.POSTED, BigDecimal.TEN, 55L);
        second.setId(3L);
        second.setInvoiceNumber("INV-002");
        when(invoiceRepository.findByStatusInAndDueDateBeforeAndDueAmountGreaterThan(
                any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(first, second)));
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(first));
        when(invoiceRepository.findById(3L)).thenReturn(Optional.of(second));
        doThrow(new IllegalStateException("notification\nfailed"))
                .when(notificationDeliveryService).deliver(eq(55L), any(), any());

        service.processDueReminders();

        verify(emailService, times(2)).send(any(User.class), any());
        verify(claimService, times(2)).markFailed(anyLong(), eq("notification\nfailed"), any());
    }

    @Test
    void emailFailureDoesNotRemoveSuccessfulInAppDelivery() {
        properties.getEmail().setEnabled(true);
        Invoice invoice = invoice(1, InvoiceStatus.POSTED, BigDecimal.TEN, 55L);
        mockInvoicePage(invoice);
        doThrow(new IllegalStateException("smtp\r\nsecret failure"))
                .when(emailService).send(any(User.class), any());

        service.processDueReminders();

        verify(notificationDeliveryService).deliver(eq(55L), any(), any());
        verify(claimService).markSent(eq(10L), any());
        verify(claimService).markFailed(eq(11L), eq("smtp\r\nsecret failure"), any());
    }

    @Test
    void retryRevalidatesAndSkipsPaidDocument() {
        properties.getEmail().setEnabled(true);
        OverdueReminderDelivery failed = delivery(
                OverdueDocumentType.INVOICE, 1L, 7, OverdueReminderChannel.EMAIL, 55L);
        failed.setStatus(OverdueReminderStatus.FAILED);
        failed.setAttemptCount(1);
        failed.setNextAttemptAt(LocalDateTime.of(2026, 8, 9, 0, 0));
        when(deliveryRepository.findRetryCandidates(any(), any(), anyInt(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(failed)));
        Invoice paid = invoice(7, InvoiceStatus.PAID, BigDecimal.ZERO, 55L);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(paid));

        service.processDueReminders();

        verify(claimService).markSkipped(anyLong(), eq("Document is no longer overdue"));
        verify(emailService, never()).send(any(), any());
    }

    private void mockInvoicePage(Invoice invoice) {
        when(invoiceRepository.findByStatusInAndDueDateBeforeAndDueAmountGreaterThan(
                any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(invoice)));
        lenient().when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
    }

    private void mockBillPage(VendorBill bill) {
        when(vendorBillRepository.findByStatusInAndDueDateBeforeAndDueAmountGreaterThan(
                any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(bill)));
        lenient().when(vendorBillRepository.findById(bill.getId())).thenReturn(Optional.of(bill));
    }

    private Invoice invoice(int daysOverdue, InvoiceStatus status, BigDecimal due, Long creatorId) {
        Invoice invoice = Invoice.builder()
                .id(1L).invoiceNumber("INV-001").invoiceDate(businessDate.minusDays(40))
                .dueDate(businessDate.minusDays(daysOverdue)).party(party(PartyType.CUSTOMER))
                .status(status).currencyCode("BDT").dueAmount(due).build();
        invoice.setCreatedBy(creatorId);
        return invoice;
    }

    private VendorBill bill(int daysOverdue, VendorBillStatus status, BigDecimal due, Long creatorId) {
        VendorBill bill = VendorBill.builder()
                .id(2L).billNumber("BILL-001").billDate(businessDate.minusDays(40))
                .dueDate(businessDate.minusDays(daysOverdue)).party(party(PartyType.VENDOR))
                .status(status).currencyCode("BDT").dueAmount(due).build();
        bill.setCreatedBy(creatorId);
        return bill;
    }

    private Party party(PartyType type) {
        return Party.builder().id(10L).name("Party").type(type).email("party@example.com").build();
    }

    private User activeUser() {
        return User.builder().id(55L).name("Creator").email("creator@example.com")
                .status(UserStatus.ACTIVE).build();
    }

    private OverdueReminderDelivery delivery(
            OverdueDocumentType type, Long documentId, Integer milestone,
            OverdueReminderChannel channel, Long recipientId
    ) {
        return OverdueReminderDelivery.builder()
                .id((long) (documentId * 10 + channel.ordinal()))
                .documentType(type).documentId(documentId).milestoneDays(milestone)
                .channel(channel).recipientUserId(recipientId)
                .status(OverdueReminderStatus.PROCESSING).attemptCount(1).build();
    }
}
