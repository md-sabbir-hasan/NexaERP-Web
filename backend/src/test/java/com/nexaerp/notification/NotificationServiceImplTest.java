package com.nexaerp.notification;

import com.nexaerp.notification.dto.NotificationResponseDto;
import com.nexaerp.security.CurrentUserService;
import com.nexaerp.user.User;
import com.nexaerp.user.UserRepository;
import com.nexaerp.user.UserStatus;
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
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
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
        user = activeUser(7L);
        lenient().when(currentUserService.getCurrentUserId()).thenReturn(7L);
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

    @Test
    void targetedSingularDoesNotResolveCurrentUserAndWaitsForCommit() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
        prepareSave();
        prepareTransactionManager();

        scheduleTargeted(7L);

        verify(currentUserService, never()).getCurrentUserId();
        verify(notificationRepository, never()).save(any());

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void targetedRollbackPersistsNothing() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();

        scheduleTargeted(7L);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization.afterCompletion(
                        TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void targetedRecipientsMustBeActiveAndPresent() {
        prepareTransactionManager();
        when(userRepository.findById(7L)).thenReturn(Optional.of(activeUser(7L)));
        when(userRepository.findById(8L)).thenReturn(Optional.of(userWithStatus(8L, UserStatus.INACTIVE)));
        when(userRepository.findById(9L)).thenReturn(Optional.of(userWithStatus(9L, UserStatus.LOCKED)));
        when(userRepository.findById(10L)).thenReturn(Optional.of(userWithStatus(10L, UserStatus.PENDING)));
        when(userRepository.findById(11L)).thenReturn(Optional.empty());
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.scheduleUniqueForUsersAfterCommit(
                Arrays.asList(7L, 8L, 9L, 10L, 11L, null),
                NotificationType.INVOICE_POSTED,
                NotificationPriority.MEDIUM,
                NotificationModule.INVOICE,
                "Invoice posted", "Posted", "/invoice/1", "INVOICE", 1L
        );

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void nullAndDuplicateRecipientsAreHandledSafely() {
        prepareTransactionManager();
        prepareSave();

        service.scheduleUniqueForUsersAfterCommit(
                null,
                NotificationType.INVOICE_POSTED,
                NotificationPriority.MEDIUM,
                NotificationModule.INVOICE,
                "Invoice posted", "Posted", "/invoice/1", "INVOICE", 1L
        );
        service.scheduleUniqueForUsersAfterCommit(
                Arrays.asList(7L, null, 7L),
                NotificationType.INVOICE_POSTED,
                NotificationPriority.MEDIUM,
                NotificationModule.INVOICE,
                "Invoice posted", "Posted", "/invoice/1", "INVOICE", 1L
        );
        scheduleTargeted(null);

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void creatorAndActorAreProcessedOnceEachOrOnceWhenSame() {
        prepareTransactionManager();
        when(userRepository.findById(7L)).thenReturn(Optional.of(activeUser(7L)));
        when(userRepository.findById(8L)).thenReturn(Optional.of(activeUser(8L)));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        scheduleTargetedUsers(Arrays.asList(7L, 8L));
        scheduleTargetedUsers(Arrays.asList(7L, 7L));

        verify(notificationRepository, times(3)).save(any(Notification.class));
        verify(notificationRepository, times(2))
                .existsByUserIdAndTypeAndEntityTypeAndEntityId(
                        7L, NotificationType.INVOICE_POSTED, "INVOICE", 1L);
        verify(notificationRepository).existsByUserIdAndTypeAndEntityTypeAndEntityId(
                8L, NotificationType.INVOICE_POSTED, "INVOICE", 1L);
    }

    @Test
    void oneRecipientFailureDoesNotBlockNextRecipient() {
        prepareTransactionManager();
        when(userRepository.findById(7L)).thenReturn(Optional.of(activeUser(7L)));
        when(userRepository.findById(8L)).thenReturn(Optional.of(activeUser(8L)));
        when(notificationRepository.save(any(Notification.class)))
                .thenThrow(new IllegalStateException("first failed"))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> scheduleTargetedUsers(Arrays.asList(7L, 8L)));

        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void repeatedTargetedEventIsSkippedForThatRecipient() {
        prepareTransactionManager();
        when(notificationRepository.existsByUserIdAndTypeAndEntityTypeAndEntityId(
                7L,
                NotificationType.INVOICE_POSTED,
                "INVOICE",
                1L
        )).thenReturn(true);

        scheduleTargeted(7L);

        verify(notificationRepository, never()).save(any());
        verify(userRepository, never()).findById(any());
        verify(currentUserService, never()).getCurrentUserId();
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

    private void scheduleTargeted(Long userId) {
        service.scheduleUniqueForUserAfterCommit(
                userId,
                NotificationType.INVOICE_POSTED,
                NotificationPriority.MEDIUM,
                NotificationModule.INVOICE,
                "Invoice posted", "Posted", "/invoice/1", "INVOICE", 1L
        );
    }

    private void scheduleTargetedUsers(List<Long> userIds) {
        service.scheduleUniqueForUsersAfterCommit(
                userIds,
                NotificationType.INVOICE_POSTED,
                NotificationPriority.MEDIUM,
                NotificationModule.INVOICE,
                "Invoice posted", "Posted", "/invoice/1", "INVOICE", 1L
        );
    }

    private User activeUser(Long id) {
        return userWithStatus(id, UserStatus.ACTIVE);
    }

    private User userWithStatus(Long id, UserStatus status) {
        return User.builder()
                .id(id)
                .name("User " + id)
                .email("user" + id + "@example.com")
                .status(status)
                .build();
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
