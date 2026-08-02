package com.nexaerp.overdue;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OverdueDeliveryClaimService {

    private final OverdueReminderDeliveryRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<OverdueReminderDelivery> claim(
            OverdueDocumentType documentType,
            Long documentId,
            int milestoneDays,
            OverdueReminderChannel channel,
            Long recipientUserId,
            LocalDateTime now,
            LocalDateTime staleBefore,
            int maxAttempts
    ) {
        repository.insertIfAbsent(
                documentType.name(), documentId, milestoneDays, channel.name(), recipientUserId, now);

        OverdueReminderDelivery delivery = repository
                .findByDocumentTypeAndDocumentIdAndMilestoneDaysAndChannelAndRecipientUserId(
                        documentType, documentId, milestoneDays, channel, recipientUserId)
                .orElseThrow(() -> new IllegalStateException("Overdue reminder claim could not be loaded"));

        if (!isClaimable(delivery, now, staleBefore, maxAttempts)) {
            return Optional.empty();
        }

        delivery.setStatus(OverdueReminderStatus.PROCESSING);
        delivery.setAttemptCount(delivery.getAttemptCount() + 1);
        delivery.setProcessingStartedAt(now);
        delivery.setNextAttemptAt(null);
        delivery.setLastError(null);
        return Optional.of(repository.save(delivery));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSent(Long deliveryId, LocalDateTime now) {
        updateProcessing(deliveryId, delivery -> {
            delivery.setStatus(OverdueReminderStatus.SENT);
            delivery.setSentAt(now);
            delivery.setProcessingStartedAt(null);
            delivery.setNextAttemptAt(null);
            delivery.setLastError(null);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSkipped(Long deliveryId, String reason) {
        updateProcessing(deliveryId, delivery -> {
            delivery.setStatus(OverdueReminderStatus.SKIPPED);
            delivery.setProcessingStartedAt(null);
            delivery.setNextAttemptAt(null);
            delivery.setLastError(sanitizeError(reason));
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long deliveryId, String error, LocalDateTime nextAttemptAt) {
        updateProcessing(deliveryId, delivery -> {
            delivery.setStatus(OverdueReminderStatus.FAILED);
            delivery.setProcessingStartedAt(null);
            delivery.setNextAttemptAt(nextAttemptAt);
            delivery.setLastError(sanitizeError(error));
        });
    }

    private boolean isClaimable(
            OverdueReminderDelivery delivery,
            LocalDateTime now,
            LocalDateTime staleBefore,
            int maxAttempts
    ) {
        if (delivery.getStatus() == OverdueReminderStatus.SENT
                || delivery.getStatus() == OverdueReminderStatus.SKIPPED
                || delivery.getAttemptCount() >= maxAttempts) {
            return false;
        }
        if (delivery.getStatus() == OverdueReminderStatus.PENDING) return true;
        if (delivery.getStatus() == OverdueReminderStatus.FAILED) {
            return delivery.getNextAttemptAt() == null || !delivery.getNextAttemptAt().isAfter(now);
        }
        return delivery.getStatus() == OverdueReminderStatus.PROCESSING
                && delivery.getProcessingStartedAt() != null
                && delivery.getProcessingStartedAt().isBefore(staleBefore);
    }

    private void updateProcessing(
            Long deliveryId,
            java.util.function.Consumer<OverdueReminderDelivery> update
    ) {
        OverdueReminderDelivery delivery = repository.findByIdForUpdate(deliveryId)
                .orElseThrow(() -> new IllegalStateException("Overdue reminder delivery not found"));
        if (delivery.getStatus() != OverdueReminderStatus.PROCESSING) return;
        update.accept(delivery);
        repository.save(delivery);
    }

    static String sanitizeError(String value) {
        if (value == null || value.isBlank()) return null;
        String sanitized = value.replaceAll("[\\p{Cntrl}]", " ").trim();
        return sanitized.length() <= 500 ? sanitized : sanitized.substring(0, 500);
    }
}
