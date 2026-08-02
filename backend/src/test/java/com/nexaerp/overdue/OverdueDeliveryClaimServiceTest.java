package com.nexaerp.overdue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OverdueDeliveryClaimServiceTest {

    @Mock private OverdueReminderDeliveryRepository repository;
    private OverdueDeliveryClaimService service;
    private final LocalDateTime now = LocalDateTime.of(2026, 8, 10, 6, 15);

    @BeforeEach
    void setUp() {
        service = new OverdueDeliveryClaimService(repository);
    }

    @Test
    void duplicateKeyRaceLoadsAndClaimsExistingPendingRow() {
        OverdueReminderDelivery delivery = delivery(OverdueReminderStatus.PENDING, 0);
        when(repository.insertIfAbsent(any(), any(), any(), any(), any(), any())).thenReturn(0);
        when(repository.findByDocumentTypeAndDocumentIdAndMilestoneDaysAndChannelAndRecipientUserId(
                any(), any(), any(), any(), any())).thenReturn(Optional.of(delivery));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<OverdueReminderDelivery> claimed = claim(delivery);

        assertTrue(claimed.isPresent());
        assertEquals(OverdueReminderStatus.PROCESSING, delivery.getStatus());
        assertEquals(1, delivery.getAttemptCount());
    }

    @Test
    void sameMilestoneSentOrSkippedIsNeverReclaimed() {
        for (OverdueReminderStatus status : new OverdueReminderStatus[]{
                OverdueReminderStatus.SENT, OverdueReminderStatus.SKIPPED}) {
            OverdueReminderDelivery delivery = delivery(status, 1);
            when(repository.findByDocumentTypeAndDocumentIdAndMilestoneDaysAndChannelAndRecipientUserId(
                    any(), any(), any(), any(), any())).thenReturn(Optional.of(delivery));
            assertFalse(claim(delivery).isPresent());
        }
    }

    @Test
    void staleProcessingCanBeReclaimedButFreshProcessingCannot() {
        OverdueReminderDelivery stale = delivery(OverdueReminderStatus.PROCESSING, 1);
        stale.setProcessingStartedAt(now.minusHours(1));
        when(repository.findByDocumentTypeAndDocumentIdAndMilestoneDaysAndChannelAndRecipientUserId(
                any(), any(), any(), any(), any())).thenReturn(Optional.of(stale));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertTrue(claim(stale).isPresent());
        assertEquals(2, stale.getAttemptCount());

        OverdueReminderDelivery fresh = delivery(OverdueReminderStatus.PROCESSING, 1);
        fresh.setProcessingStartedAt(now.minusMinutes(5));
        when(repository.findByDocumentTypeAndDocumentIdAndMilestoneDaysAndChannelAndRecipientUserId(
                any(), any(), any(), any(), any())).thenReturn(Optional.of(fresh));
        assertFalse(claim(fresh).isPresent());
    }

    @Test
    void failedRetryRespectsNextAttemptAndMaximumAttempts() {
        OverdueReminderDelivery due = delivery(OverdueReminderStatus.FAILED, 1);
        due.setNextAttemptAt(now.minusMinutes(1));
        when(repository.findByDocumentTypeAndDocumentIdAndMilestoneDaysAndChannelAndRecipientUserId(
                any(), any(), any(), any(), any())).thenReturn(Optional.of(due));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertTrue(claim(due).isPresent());

        OverdueReminderDelivery future = delivery(OverdueReminderStatus.FAILED, 1);
        future.setNextAttemptAt(now.plusMinutes(1));
        when(repository.findByDocumentTypeAndDocumentIdAndMilestoneDaysAndChannelAndRecipientUserId(
                any(), any(), any(), any(), any())).thenReturn(Optional.of(future));
        assertFalse(claim(future).isPresent());

        OverdueReminderDelivery exhausted = delivery(OverdueReminderStatus.FAILED, 3);
        exhausted.setNextAttemptAt(now.minusDays(1));
        when(repository.findByDocumentTypeAndDocumentIdAndMilestoneDaysAndChannelAndRecipientUserId(
                any(), any(), any(), any(), any())).thenReturn(Optional.of(exhausted));
        assertFalse(claim(exhausted).isPresent());
    }

    @Test
    void statusUpdatesAreIndependentAndErrorsAreSanitizedAndTruncated() {
        OverdueReminderDelivery delivery = delivery(OverdueReminderStatus.PROCESSING, 1);
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(delivery));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.markFailed(1L, "SMTP\n" + "x".repeat(600), now.plusDays(1));

        assertEquals(OverdueReminderStatus.FAILED, delivery.getStatus());
        assertEquals(500, delivery.getLastError().length());
        assertFalse(delivery.getLastError().contains("\n"));
        verify(repository).save(delivery);
    }

    private Optional<OverdueReminderDelivery> claim(OverdueReminderDelivery delivery) {
        return service.claim(
                delivery.getDocumentType(), delivery.getDocumentId(), delivery.getMilestoneDays(),
                delivery.getChannel(), delivery.getRecipientUserId(), now, now.minusMinutes(30), 3);
    }

    private OverdueReminderDelivery delivery(OverdueReminderStatus status, int attempts) {
        return OverdueReminderDelivery.builder()
                .id(1L).documentType(OverdueDocumentType.INVOICE).documentId(10L)
                .milestoneDays(7).channel(OverdueReminderChannel.EMAIL).recipientUserId(55L)
                .status(status).attemptCount(attempts).build();
    }
}
