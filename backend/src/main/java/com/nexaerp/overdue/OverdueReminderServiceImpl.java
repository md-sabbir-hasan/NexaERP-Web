package com.nexaerp.overdue;

import com.nexaerp.email.OverdueAlertEmailService;
import com.nexaerp.invoice.Invoice;
import com.nexaerp.invoice.InvoiceRepository;
import com.nexaerp.invoice.InvoiceStatus;
import com.nexaerp.notification.NotificationModule;
import com.nexaerp.notification.NotificationType;
import com.nexaerp.user.User;
import com.nexaerp.user.UserRepository;
import com.nexaerp.user.UserStatus;
import com.nexaerp.vendorbill.VendorBill;
import com.nexaerp.vendorbill.VendorBillRepository;
import com.nexaerp.vendorbill.VendorBillStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OverdueReminderServiceImpl implements OverdueReminderService {

    private static final long MISSING_RECIPIENT_ID = 0L;
    private static final List<InvoiceStatus> INVOICE_STATUSES =
            List.of(InvoiceStatus.POSTED, InvoiceStatus.PARTIAL);
    private static final List<VendorBillStatus> VENDOR_BILL_STATUSES =
            List.of(VendorBillStatus.POSTED, VendorBillStatus.PARTIAL);

    private final InvoiceRepository invoiceRepository;
    private final VendorBillRepository vendorBillRepository;
    private final OverdueReminderDeliveryRepository deliveryRepository;
    private final OverdueDeliveryClaimService claimService;
    private final OverdueNotificationDeliveryService notificationDeliveryService;
    private final OverdueAlertEmailService emailService;
    private final UserRepository userRepository;
    private final OverdueMessageFormatter formatter;
    private final OverdueEligibility eligibility;
    private final OverdueProperties properties;
    private final Clock overdueClock;

    @Override
    public void processDueReminders() {
        if (!properties.isEnabled()) return;

        LocalDate businessDate = LocalDate.now(overdueClock);
        LocalDateTime now = LocalDateTime.now(overdueClock);
        processRetryCandidates(businessDate, now);

        if (properties.getInvoice().isEnabled()) processInvoices(businessDate, now);
        if (properties.getVendorBill().isEnabled()) processVendorBills(businessDate, now);
    }

    private void processInvoices(LocalDate businessDate, LocalDateTime now) {
        int pageNumber = 0;
        Page<Invoice> page;
        do {
            page = invoiceRepository.findByStatusInAndDueDateBeforeAndDueAmountGreaterThan(
                    INVOICE_STATUSES, businessDate, BigDecimal.ZERO,
                    PageRequest.of(pageNumber++, properties.getBatchSize()));
            for (Invoice invoice : page.getContent()) {
                runSafely(OverdueDocumentType.INVOICE, invoice.getId(), () ->
                        highestMilestone(daysOverdue(invoice.getDueDate(), businessDate))
                                .ifPresent(milestone -> processDocument(
                                        snapshot(invoice, businessDate), milestone, now)));
            }
        } while (page.hasNext());
    }

    private void processVendorBills(LocalDate businessDate, LocalDateTime now) {
        int pageNumber = 0;
        Page<VendorBill> page;
        do {
            page = vendorBillRepository.findByStatusInAndDueDateBeforeAndDueAmountGreaterThan(
                    VENDOR_BILL_STATUSES, businessDate, BigDecimal.ZERO,
                    PageRequest.of(pageNumber++, properties.getBatchSize()));
            for (VendorBill bill : page.getContent()) {
                runSafely(OverdueDocumentType.VENDOR_BILL, bill.getId(), () ->
                        highestMilestone(daysOverdue(bill.getDueDate(), businessDate))
                                .ifPresent(milestone -> processDocument(
                                        snapshot(bill, businessDate), milestone, now)));
            }
        } while (page.hasNext());
    }

    private void processRetryCandidates(LocalDate businessDate, LocalDateTime now) {
        while (true) {
            Page<OverdueReminderDelivery> candidates = deliveryRepository.findRetryCandidates(
                    now,
                    now.minus(properties.getProcessingTimeout()),
                    properties.getEmail().getMaxAttempts(),
                    PageRequest.of(0, properties.getBatchSize()));
            if (candidates.isEmpty()) return;
            for (OverdueReminderDelivery candidate : candidates) {
                if ((candidate.getChannel() == OverdueReminderChannel.EMAIL
                        && !properties.getEmail().isEnabled())
                        || (candidate.getChannel() == OverdueReminderChannel.IN_APP
                        && !properties.getInApp().isEnabled())) {
                    runSafely(candidate.getDocumentType(), candidate.getDocumentId(), () ->
                            claimAndSkip(candidate, now, "Delivery channel is disabled"));
                    continue;
                }
                runSafely(candidate.getDocumentType(), candidate.getDocumentId(), () ->
                        loadSnapshot(candidate.getDocumentType(), candidate.getDocumentId(), businessDate)
                                .ifPresentOrElse(
                                        document -> processChannel(
                                                document,
                                                candidate.getMilestoneDays(),
                                                candidate.getChannel(),
                                                candidate.getRecipientUserId(),
                                                now),
                                        () -> claimAndSkip(candidate, now, "Document is no longer overdue")
                                ));
            }
            if (!candidates.hasNext()) return;
        }
    }

    private void processDocument(OverdueDocumentSnapshot document, int milestone, LocalDateTime now) {
        Long recipientId = document.creatorUserId() != null
                ? document.creatorUserId()
                : MISSING_RECIPIENT_ID;
        if (properties.getInApp().isEnabled()) {
            processChannel(document, milestone, OverdueReminderChannel.IN_APP, recipientId, now);
        }
        if (properties.getEmail().isEnabled()) {
            processChannel(document, milestone, OverdueReminderChannel.EMAIL, recipientId, now);
        }
    }

    private void processChannel(
            OverdueDocumentSnapshot discovered,
            int milestone,
            OverdueReminderChannel channel,
            Long recipientId,
            LocalDateTime now
    ) {
        int maxAttempts = properties.getEmail().getMaxAttempts();
        Optional<OverdueReminderDelivery> claimed = claimService.claim(
                discovered.documentType(), discovered.documentId(), milestone, channel, recipientId,
                now, now.minus(properties.getProcessingTimeout()), maxAttempts);
        if (claimed.isEmpty()) return;

        OverdueReminderDelivery delivery = claimed.get();
        Optional<OverdueDocumentSnapshot> revalidated = loadSnapshot(
                discovered.documentType(), discovered.documentId(), LocalDate.now(overdueClock));
        if (revalidated.isEmpty()) {
            claimService.markSkipped(delivery.getId(), "Document is no longer overdue");
            return;
        }
        OverdueDocumentSnapshot document = revalidated.get();
        if (document.creatorUserId() == null || !document.creatorUserId().equals(recipientId)) {
            claimService.markSkipped(delivery.getId(), "Document creator is unavailable or changed");
            return;
        }

        User recipient = userRepository.findById(recipientId).orElse(null);
        if (recipient == null || recipient.getStatus() != UserStatus.ACTIVE) {
            claimService.markSkipped(delivery.getId(), "Recipient user is missing or not active");
            return;
        }

        try {
            if (channel == OverdueReminderChannel.IN_APP) {
                boolean sent = notificationDeliveryService.deliver(
                        recipientId, document, formatter.notificationMessage(document));
                if (!sent) {
                    claimService.markSkipped(delivery.getId(), "Recipient user is missing or not active");
                    return;
                }
            } else {
                if (recipient.getEmail() == null || recipient.getEmail().isBlank()) {
                    claimService.markSkipped(delivery.getId(), "Recipient email is blank");
                    return;
                }
                emailService.send(recipient, document);
            }
            claimService.markSent(delivery.getId(), now);
        } catch (RuntimeException exception) {
            claimService.markFailed(
                    delivery.getId(), exception.getMessage(), now.plus(properties.getEmail().getRetryDelay()));
            log.warn("Overdue {} delivery failed for {}:{}",
                    channel, document.documentType(), document.documentId(), exception);
        }
    }

    private void claimAndSkip(
            OverdueReminderDelivery candidate,
            LocalDateTime now,
            String reason
    ) {
        claimService.claim(
                        candidate.getDocumentType(), candidate.getDocumentId(), candidate.getMilestoneDays(),
                        candidate.getChannel(), candidate.getRecipientUserId(), now,
                        now.minus(properties.getProcessingTimeout()), properties.getEmail().getMaxAttempts())
                .ifPresent(delivery -> claimService.markSkipped(delivery.getId(), reason));
    }

    private Optional<OverdueDocumentSnapshot> loadSnapshot(
            OverdueDocumentType type,
            Long documentId,
            LocalDate businessDate
    ) {
        if (type == OverdueDocumentType.INVOICE) {
            return invoiceRepository.findById(documentId)
                    .filter(invoice -> eligibility.isEligible(invoice, businessDate))
                    .map(invoice -> snapshot(invoice, businessDate));
        }
        return vendorBillRepository.findById(documentId)
                .filter(bill -> eligibility.isEligible(bill, businessDate))
                .map(bill -> snapshot(bill, businessDate));
    }

    private OverdueDocumentSnapshot snapshot(Invoice invoice, LocalDate businessDate) {
        return new OverdueDocumentSnapshot(
                OverdueDocumentType.INVOICE, invoice.getId(), invoice.getInvoiceNumber(),
                invoice.getParty().getName(), invoice.getDueDate(), invoice.getDueAmount(),
                invoice.getCurrencyCode(), invoice.getCreatedBy(), NotificationType.INVOICE_OVERDUE,
                NotificationModule.INVOICE, "Invoice overdue", "/invoice/" + invoice.getId(),
                "INVOICE", daysOverdue(invoice.getDueDate(), businessDate));
    }

    private OverdueDocumentSnapshot snapshot(VendorBill bill, LocalDate businessDate) {
        return new OverdueDocumentSnapshot(
                OverdueDocumentType.VENDOR_BILL, bill.getId(), bill.getBillNumber(),
                bill.getParty().getName(), bill.getDueDate(), bill.getDueAmount(),
                bill.getCurrencyCode(), bill.getCreatedBy(), NotificationType.VENDOR_BILL_OVERDUE,
                NotificationModule.VENDOR_BILL, "Vendor bill overdue", "/vendor-bill/" + bill.getId(),
                "VENDOR_BILL", daysOverdue(bill.getDueDate(), businessDate));
    }

    private Optional<Integer> highestMilestone(long daysOverdue) {
        return properties.getMilestones().stream()
                .filter(milestone -> milestone <= daysOverdue)
                .max(Integer::compareTo);
    }

    private long daysOverdue(LocalDate dueDate, LocalDate businessDate) {
        return ChronoUnit.DAYS.between(dueDate, businessDate);
    }

    private void runSafely(OverdueDocumentType type, Long id, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            log.warn("Overdue reminder processing failed for {}:{}", type, id, exception);
        }
    }
}
