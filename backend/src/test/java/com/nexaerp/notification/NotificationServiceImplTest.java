package com.nexaerp.notification;

import com.nexaerp.notification.dto.NotificationResponseDto;
import com.nexaerp.security.CurrentUserService;
import com.nexaerp.user.User;
import com.nexaerp.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private UserRepository userRepository;
    @Mock private PlatformTransactionManager transactionManager;

    private NotificationServiceImpl service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new NotificationServiceImpl(
                notificationRepository,
                currentUserService,
                userRepository,
                transactionManager
        );
        user = User.builder().id(7L).name("Test User").email("test@example.com").build();
        when(currentUserService.getCurrentUserId()).thenReturn(7L);
    }

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void legacyCreationDefaultsPriorityAndModule() {
        prepareSave();

        NotificationResponseDto response = service.createForCurrentUser(
                NotificationType.SYSTEM,
                "System update",
                "Legacy-compatible notification",
                null,
                "SYSTEM",
                null
        );

        assertEquals(NotificationPriority.MEDIUM, response.getPriority());
        assertEquals(NotificationModule.SYSTEM, response.getModule());
    }

    @Test
    void explicitCreationMapsPriorityAndModule() {
        prepareSave();

        NotificationResponseDto response = service.createForCurrentUser(
                NotificationType.ACCOUNTING_PERIOD_LOCKED,
                NotificationPriority.CRITICAL,
                NotificationModule.ACCOUNTING_PERIOD,
                "Accounting period locked",
                "July 2026 was locked",
                "/accounting-periods",
                "ACCOUNTING_PERIOD",
                11L
        );

        assertEquals(NotificationPriority.CRITICAL, response.getPriority());
        assertEquals(NotificationModule.ACCOUNTING_PERIOD, response.getModule());
    }

    @Test
    void bulkMarkAllIsScopedToCurrentUser() {
        service.markAllAsRead();

        verify(notificationRepository).markAllAsReadByUserId(
                org.mockito.ArgumentMatchers.eq(7L),
                any(LocalDateTime.class)
        );
        verify(notificationRepository, never()).findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(
                any(), any()
        );
    }

    @Test
    void afterCommitNotificationIsNotSavedBeforeCommit() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
        prepareSave();
        prepareTransactionManager();

        scheduleUniqueJournal();

        verify(notificationRepository, never()).save(any());

        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
        synchronizations.forEach(TransactionSynchronization::afterCommit);

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void rolledBackOperationDoesNotCreateNotification() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();

        scheduleUniqueJournal();
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization.afterCompletion(
                        TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void notificationFailureAfterCommitDoesNotEscape() {
        prepareTransactionManager();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(notificationRepository.save(any(Notification.class)))
                .thenThrow(new IllegalStateException("database unavailable"));

        assertDoesNotThrow(this::scheduleUniqueJournal);
    }

    @Test
    void uniqueEventIsSkippedWhenIdentityAlreadyExists() {
        prepareTransactionManager();
        when(notificationRepository.existsByUserIdAndTypeAndEntityTypeAndEntityId(
                7L,
                NotificationType.JOURNAL_DRAFT_PENDING,
                "JOURNAL",
                21L
        )).thenReturn(true);

        scheduleUniqueJournal();

        verify(notificationRepository, never()).save(any());
        verify(userRepository, never()).findById(any());
    }

    private void scheduleUniqueJournal() {
        service.scheduleUniqueForCurrentUserAfterCommit(
                NotificationType.JOURNAL_DRAFT_PENDING,
                NotificationPriority.MEDIUM,
                NotificationModule.JOURNAL,
                "Journal draft created",
                "Journal JE-0001 was created as a draft.",
                "/journals/21/edit",
                "JOURNAL",
                21L
        );
    }

    private void prepareSave() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(1L);
            notification.setCreatedAt(LocalDateTime.now());
            return notification;
        });
    }

    private void prepareTransactionManager() {
        when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(new SimpleTransactionStatus());
    }
}
