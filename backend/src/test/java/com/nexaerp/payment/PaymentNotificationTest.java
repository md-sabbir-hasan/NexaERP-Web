package com.nexaerp.payment;

import com.nexaerp.account.Account;
import com.nexaerp.account.AccountRepository;
import com.nexaerp.account.AccountType;
import com.nexaerp.accountingperiod.AccountingPeriodService;
import com.nexaerp.approval.ApprovalService;
import com.nexaerp.approval.ApprovalRequest;
import com.nexaerp.audit.AuditAction;
import com.nexaerp.audit.AuditLogService;
import com.nexaerp.banking.entity.BankAccount;
import com.nexaerp.banking.entity.BankTransaction;
import com.nexaerp.banking.repository.BankAccountRepository;
import com.nexaerp.banking.repository.BankTransactionRepository;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.expense.ExpenseRepository;
import com.nexaerp.invoice.InvoiceRepository;
import com.nexaerp.invoice.Invoice;
import com.nexaerp.invoice.InvoiceStatus;
import com.nexaerp.journal.JournalEntry;
import com.nexaerp.journal.JournalEntryRepository;
import com.nexaerp.journal.JournalLine;
import com.nexaerp.journal.JournalLineRepository;
import com.nexaerp.journal.JournalSourceType;
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
import com.nexaerp.vendorbill.VendorBillRepository;
import com.nexaerp.payment.dto.PaymentAllocationRequestDto;
import com.nexaerp.payment.dto.PaymentRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentNotificationTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentAllocationRepository paymentAllocationRepository;
    @Mock private PartyRepository partyRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private VendorBillRepository vendorBillRepository;
    @Mock private JournalEntryRepository journalEntryRepository;
    @Mock private JournalLineRepository journalLineRepository;
    @Mock private SystemSettingsService systemSettingsService;
    @Mock private AuditLogService auditLogService;
    @Mock private AccountingPeriodService accountingPeriodService;
    @Mock private MakerCheckerService makerCheckerService;
    @Mock private CurrentUserService currentUserService;
    @Mock private BankAccountRepository bankAccountRepository;
    @Mock private BankTransactionRepository bankTransactionRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private NotificationService notificationService;
    @Mock private ApprovalService approvalService;

    @InjectMocks private PaymentServiceImpl service;

    private Account paymentAccount;
    private Account receivable;
    private Account payable;

    @BeforeEach
    void setUp() {
        paymentAccount = account(10L, AccountType.ASSET, new BigDecimal("1000.00"));
        receivable = account(20L, AccountType.ASSET, BigDecimal.ZERO);
        payable = account(30L, AccountType.LIABILITY, BigDecimal.ZERO);

        lenient().when(systemSettingsService.getAccount(SettingKey.DEFAULT_RECEIVABLE_ACCOUNT))
                .thenReturn(receivable);
        lenient().when(systemSettingsService.getAccount(SettingKey.DEFAULT_PAYABLE_ACCOUNT))
                .thenReturn(payable);
        lenient().when(currentUserService.getCurrentUserId()).thenReturn(99L);
        lenient().when(journalEntryRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        lenient().when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(invocation -> {
            JournalEntry entry = invocation.getArgument(0);
            if (entry.getId() == null) entry.setId(100L);
            return entry;
        });
        lenient().when(journalLineRepository.save(any(JournalLine.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(paymentAllocationRepository.findByPaymentId(1L)).thenReturn(List.of());
        lenient().when(approvalService.lockAndValidatePaymentForPosting(1L)).thenReturn(null);
        lenient().when(approvalService.isPaymentApprovalEnabled()).thenReturn(false);
        lenient().when(bankAccountRepository.findByCoaAccountId(10L))
                .thenReturn(Optional.of(BankAccount.builder()
                        .id(40L).coaAccountId(10L).currency("BDT").isActive(true)
                        .currentBalance(new BigDecimal("1000.00")).build()));
        lenient().when(bankAccountRepository.findByCoaAccountIdForUpdate(10L))
                .thenReturn(Optional.of(BankAccount.builder()
                        .id(40L).coaAccountId(10L).currency("BDT").isActive(true)
                        .currentBalance(new BigDecimal("1000.00")).build()));
        lenient().when(bankTransactionRepository.findByReferenceNumber(any())).thenReturn(Optional.empty());
        lenient().when(bankTransactionRepository.save(any(BankTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void receiptPostSchedulesCreatorAndActorWithExactMetadata() {
        Payment payment = postablePayment(PaymentType.RECEIPT, 55L);
        ApprovalRequest approval = ApprovalRequest.builder().id(70L).build();
        when(approvalService.lockAndValidatePaymentForPosting(1L)).thenReturn(approval);

        service.post(1L);

        assertEquals(PaymentStatus.POSTED, payment.getStatus());
        verifyPostedNotification(55L, 99L);
        verify(approvalService).consumeAfterSuccessfulPost(approval);
    }

    @Test
    void draftCancellationCancelsActiveApprovalAfterPaymentAudit() {
        Payment payment = postablePayment(PaymentType.RECEIPT, 55L);
        ApprovalRequest approval = ApprovalRequest.builder().id(71L).build();
        when(approvalService.lockActivePaymentForCancellation(1L)).thenReturn(approval);

        service.cancel(1L);

        assertEquals(PaymentStatus.CANCELLED, payment.getStatus());
        verify(auditLogService).log(AuditAction.CANCELLED, "PAYMENT", 1L, "DRAFT", "CANCELLED");
        verify(approvalService).cancelAfterSuccessfulDocumentCancellation(approval);
    }

    @Test
    void vendorPaymentPostUsesTheSameLifecycleNotification() {
        Payment payment = postablePayment(PaymentType.PAYMENT, 55L);

        service.post(1L);

        assertEquals(PaymentStatus.POSTED, payment.getStatus());
        verifyPostedNotification(55L, 99L);
    }

    @Test
    void manualAllocationRejectsDraftInvoice() {
        Party party = Party.builder().id(5L).name("Customer").type(PartyType.CUSTOMER).isActive(true).build();
        Invoice invoice = Invoice.builder().id(7L).party(party).status(InvoiceStatus.DRAFT)
                .grandTotal(new BigDecimal("100.00")).dueAmount(new BigDecimal("100.00")).build();
        when(partyRepository.findById(5L)).thenReturn(Optional.of(party));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(paymentAccount));
        when(invoiceRepository.findById(7L)).thenReturn(Optional.of(invoice));
        PaymentRequestDto request = new PaymentRequestDto(5L, 10L, LocalDate.of(2026, 8, 3),
                PaymentType.RECEIPT, new BigDecimal("25.00"), "BDT", PaymentMethod.CASH,
                null, null, false, List.of(new PaymentAllocationRequestDto(
                PaymentReferenceType.INVOICE, 7L, new BigDecimal("25.00"))));

        assertThrows(BusinessRuleException.class, () -> service.create(request));
    }

    @Test
    void fifoUsesOnlyPostedAndPartialInvoiceStatuses() {
        Party party = Party.builder().id(5L).name("Customer").type(PartyType.CUSTOMER).isActive(true).build();
        when(partyRepository.findById(5L)).thenReturn(Optional.of(party));
        when(accountRepository.findById(10L)).thenReturn(Optional.of(paymentAccount));
        when(invoiceRepository.findByPartyIdAndDueAmountGreaterThanAndStatusInOrderByDueDateAsc(
                5L, BigDecimal.ZERO, List.of(InvoiceStatus.POSTED, InvoiceStatus.PARTIAL))).thenReturn(List.of());
        PaymentRequestDto request = new PaymentRequestDto(5L, 10L, LocalDate.of(2026, 8, 3),
                PaymentType.RECEIPT, new BigDecimal("25.00"), "BDT", PaymentMethod.CASH,
                null, null, true, List.of());

        service.create(request);

        verify(invoiceRepository).findByPartyIdAndDueAmountGreaterThanAndStatusInOrderByDueDateAsc(
                5L, BigDecimal.ZERO, List.of(InvoiceStatus.POSTED, InvoiceStatus.PARTIAL));
    }

    @Test
    void postingRejectsStoredAllocationWhenInvoiceIsNoLongerEligible() {
        Payment payment = postablePayment(PaymentType.RECEIPT, 55L);
        Invoice draft = Invoice.builder().id(7L).status(InvoiceStatus.DRAFT)
                .dueAmount(new BigDecimal("100.00")).build();
        PaymentAllocation allocation = PaymentAllocation.builder()
                .payment(payment)
                .referenceType(PaymentReferenceType.INVOICE)
                .referenceId(7L)
                .allocatedAmount(new BigDecimal("25.00"))
                .build();
        payment.setAllocatedAmount(new BigDecimal("25.00"));
        payment.setUnallocatedAmount(new BigDecimal("75.00"));
        when(paymentAllocationRepository.findByPaymentId(1L)).thenReturn(List.of(allocation));
        when(invoiceRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(draft));

        assertThrows(BusinessRuleException.class, () -> service.post(1L));

        verifyNoPostedNotification();
    }

    @Test
    void sameCreatorAndActorAndUnallocatedRemainderArePassedSafely() {
        Payment payment = postablePayment(PaymentType.RECEIPT, 99L);

        service.post(1L);

        verifyPostedNotification(99L, 99L);
    }

    @Test
    void invalidStatusesAndDuplicateJournalScheduleNothing() {
        for (PaymentStatus status : List.of(PaymentStatus.POSTED, PaymentStatus.CANCELLED)) {
            Payment payment = payment(PaymentType.RECEIPT, status, 55L);
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
            assertThrows(BusinessRuleException.class, () -> service.post(1L));
        }

        Payment duplicate = payment(PaymentType.RECEIPT, PaymentStatus.DRAFT, 55L);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(duplicate));
        when(journalEntryRepository.findBySourceTypeAndSourceId(JournalSourceType.PAYMENT, 1L))
                .thenReturn(Optional.of(new JournalEntry()));
        assertThrows(BusinessRuleException.class, () -> service.post(1L));

        verifyNoPostedNotification();
    }

    @Test
    void makerCheckerAndClosedPeriodFailuresScheduleNothing() {
        Payment payment = postablePayment(PaymentType.RECEIPT, 55L);
        doThrow(new BusinessRuleException("maker-checker"))
                .when(makerCheckerService).validateChecker(55L, "Payment");
        assertThrows(BusinessRuleException.class, () -> service.post(1L));
        verifyNoPostedNotification();

        org.mockito.Mockito.reset(makerCheckerService);
        doThrow(new BusinessRuleException("closed period"))
                .when(accountingPeriodService).validatePostingDate(payment.getPaymentDate());
        assertThrows(BusinessRuleException.class, () -> service.post(1L));
        verifyNoPostedNotification();
    }

    @Test
    void insufficientBalanceAndLinkedBankFailuresScheduleNothing() {
        postablePayment(PaymentType.PAYMENT, 55L);
        when(bankAccountRepository.findByCoaAccountIdForUpdate(10L))
                .thenReturn(Optional.of(BankAccount.builder()
                        .id(40L).coaAccountId(10L).currency("BDT").isActive(true)
                        .currentBalance(new BigDecimal("10.00")).build()));
        assertThrows(BusinessRuleException.class, () -> service.post(1L));
        verifyNoPostedNotification();

        postablePayment(PaymentType.RECEIPT, 55L);
        when(bankAccountRepository.findByCoaAccountIdForUpdate(10L)).thenReturn(Optional.empty());
        assertThrows(BusinessRuleException.class, () -> service.post(1L));
        verifyNoPostedNotification();
    }

    @Test
    void journalBankTransactionAllocationAndAuditFailuresScheduleNothing() {
        postablePayment(PaymentType.RECEIPT, 55L);
        doThrow(new IllegalStateException("COA save failed"))
                .when(accountRepository).save(any(Account.class));
        assertThrows(IllegalStateException.class, () -> service.post(1L));
        verifyNoPostedNotification();
        verify(approvalService, never()).consumeAfterSuccessfulPost(any());

        org.mockito.Mockito.reset(accountRepository);
        postablePayment(PaymentType.RECEIPT, 55L);
        doThrow(new IllegalStateException("bank transaction failed"))
                .when(bankTransactionRepository).save(any(BankTransaction.class));
        assertThrows(IllegalStateException.class, () -> service.post(1L));
        verifyNoPostedNotification();

        org.mockito.Mockito.reset(bankTransactionRepository);
        postablePayment(PaymentType.RECEIPT, 55L);
        PaymentAllocation stale = PaymentAllocation.builder()
                .payment(paymentRepository.findById(1L).orElseThrow())
                .referenceType(PaymentReferenceType.INVOICE)
                .referenceId(999L)
                .allocatedAmount(BigDecimal.ONE)
                .build();
        Payment stalePayment = paymentRepository.findById(1L).orElseThrow();
        stalePayment.setAllocatedAmount(BigDecimal.ONE);
        stalePayment.setUnallocatedAmount(new BigDecimal("99.00"));
        when(paymentAllocationRepository.findByPaymentId(1L)).thenReturn(List.of(stale));
        when(invoiceRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.post(1L));
        verifyNoPostedNotification();

        when(paymentAllocationRepository.findByPaymentId(1L)).thenReturn(List.of());
        postablePayment(PaymentType.RECEIPT, 55L);
        doThrow(new IllegalStateException("audit failed")).when(auditLogService).log(
                AuditAction.POSTED, "PAYMENT", 1L, "DRAFT", "POSTED");
        assertThrows(IllegalStateException.class, () -> service.post(1L));
        verifyNoPostedNotification();
    }

    private Payment postablePayment(PaymentType type, Long createdBy) {
        Payment payment = payment(type, PaymentStatus.DRAFT, createdBy);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        lenient().when(journalEntryRepository.findBySourceTypeAndSourceId(JournalSourceType.PAYMENT, 1L))
                .thenReturn(Optional.empty());
        return payment;
    }

    private Payment payment(PaymentType type, PaymentStatus status, Long createdBy) {
        PartyType partyType = type == PaymentType.RECEIPT ? PartyType.CUSTOMER : PartyType.VENDOR;
        Payment payment = Payment.builder()
                .id(1L)
                .paymentNumber("PAY-2026-000001")
                .paymentDate(LocalDate.of(2026, 8, 2))
                .paymentType(type)
                .party(Party.builder().id(5L).name("Party").type(partyType).isActive(true).build())
                .account(paymentAccount)
                .amount(new BigDecimal("100.00"))
                .allocatedAmount(BigDecimal.ZERO)
                .unallocatedAmount(new BigDecimal("100.00"))
                .currencyCode("BDT")
                .exchangeRate(BigDecimal.ONE)
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .status(status)
                .build();
        payment.setCreatedBy(createdBy);
        return payment;
    }

    private Account account(Long id, AccountType type, BigDecimal balance) {
        Account account = new Account();
        account.setId(id);
        account.setCode("A-" + id);
        account.setName("Account " + id);
        account.setType(type);
        account.setIsActive(true);
        account.setCurrentBalance(balance);
        return account;
    }

    private void verifyPostedNotification(Long creatorId, Long actorId) {
        verify(notificationService).scheduleUniqueForUsersAfterCommit(
                Arrays.asList(creatorId, actorId),
                NotificationType.PAYMENT_POSTED,
                NotificationPriority.MEDIUM,
                NotificationModule.PAYMENT,
                "Payment posted",
                "Payment PAY-2026-000001 was posted successfully.",
                "/payment/1",
                "PAYMENT",
                1L
        );
    }

    private void verifyNoPostedNotification() {
        verify(notificationService, never()).scheduleUniqueForUsersAfterCommit(
                any(), any(), any(), any(), any(), any(), any(), any(), any());
    }
}
