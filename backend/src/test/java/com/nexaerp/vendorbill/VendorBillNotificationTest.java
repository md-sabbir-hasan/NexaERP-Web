package com.nexaerp.vendorbill;

import com.nexaerp.account.Account;
import com.nexaerp.account.AccountRepository;
import com.nexaerp.account.AccountType;
import com.nexaerp.accountingperiod.AccountingPeriodService;
import com.nexaerp.approval.ApprovalService;
import com.nexaerp.audit.AuditAction;
import com.nexaerp.audit.AuditLogService;
import com.nexaerp.budget.BudgetCheckService;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.costcenter.CostCenterService;
import com.nexaerp.email.BudgetAlertEmailService;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
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
class VendorBillNotificationTest {

    @Mock private VendorBillRepository vendorBillRepository;
    @Mock private VendorBillItemRepository vendorBillItemRepository;
    @Mock private PartyRepository partyRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private JournalEntryRepository journalEntryRepository;
    @Mock private JournalLineRepository journalLineRepository;
    @Mock private SystemSettingsService systemSettingsService;
    @Mock private AuditLogService auditLogService;
    @Mock private AccountingPeriodService accountingPeriodService;
    @Mock private MakerCheckerService makerCheckerService;
    @Mock private CurrentUserService currentUserService;
    @Mock private BudgetCheckService budgetCheckService;
    @Mock private NotificationService notificationService;
    @Mock private BudgetAlertEmailService budgetAlertEmailService;
    @Mock private CostCenterService costCenterService;
    @Mock private ApprovalService approvalService;

    @InjectMocks private VendorBillServiceImpl service;

    private Account expense;
    private Account payable;
    private Account inputVat;
    private Account tdsPayable;

    @BeforeEach
    void setUp() {
        lenient().when(approvalService.lockAndValidateVendorBillForPosting(any())).thenReturn(null);
        lenient().when(approvalService.lockActiveVendorBillForCancellation(any())).thenReturn(null);
        expense = account(10L, AccountType.EXPENSE);
        payable = account(20L, AccountType.LIABILITY);
        inputVat = account(30L, AccountType.ASSET);
        tdsPayable = account(40L, AccountType.LIABILITY);

        lenient().when(systemSettingsService.getAccount(SettingKey.DEFAULT_PAYABLE_ACCOUNT))
                .thenReturn(payable);
        lenient().when(systemSettingsService.getAccount(SettingKey.DEFAULT_INPUT_VAT))
                .thenReturn(inputVat);
        lenient().when(systemSettingsService.getAccount(SettingKey.DEFAULT_TDS_PAYABLE))
                .thenReturn(tdsPayable);
        lenient().when(currentUserService.getCurrentUserId()).thenReturn(99L);
        lenient().when(journalEntryRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        lenient().when(journalEntryRepository.save(any(JournalEntry.class)))
                .thenAnswer(invocation -> {
                    JournalEntry journal = invocation.getArgument(0);
                    if (journal.getId() == null) journal.setId(100L);
                    return journal;
                });
        lenient().when(vendorBillRepository.save(any(VendorBill.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(budgetCheckService.checkExpenseAccount(any(), any(), any()))
                .thenReturn(Optional.empty());
    }

    @Test
    void successfulPostSchedulesCreatorAndActorWithExactMetadata() {
        VendorBill bill = postableBill();

        service.post(1L);

        assertEquals(VendorBillStatus.POSTED, bill.getStatus());
        verify(notificationService).scheduleUniqueForUsersAfterCommit(
                Arrays.asList(55L, 99L),
                NotificationType.VENDOR_BILL_POSTED,
                NotificationPriority.MEDIUM,
                NotificationModule.VENDOR_BILL,
                "Vendor bill posted",
                "Vendor bill BILL-2026-000001 was posted successfully.",
                "/vendor-bill/1",
                "VENDOR_BILL",
                1L
        );
    }

    @Test
    void samePostCreatorAndActorIsPassedSafely() {
        VendorBill bill = postableBill();
        bill.setCreatedBy(99L);

        service.post(1L);

        verify(notificationService).scheduleUniqueForUsersAfterCommit(
                Arrays.asList(99L, 99L),
                NotificationType.VENDOR_BILL_POSTED,
                NotificationPriority.MEDIUM,
                NotificationModule.VENDOR_BILL,
                "Vendor bill posted",
                "Vendor bill BILL-2026-000001 was posted successfully.",
                "/vendor-bill/1", "VENDOR_BILL", 1L
        );
    }

    @Test
    void repeatedNonApprovedAndCancelledPostsScheduleNothing() {
        for (VendorBillStatus status : List.of(
                VendorBillStatus.POSTED,
                VendorBillStatus.DRAFT,
                VendorBillStatus.CANCELLED
        )) {
            VendorBill bill = bill(status, BigDecimal.ZERO);
            when(vendorBillRepository.findById(1L)).thenReturn(Optional.of(bill));
            assertThrows(BusinessRuleException.class, () -> service.post(1L));
        }

        verifyNoLifecycleNotification();
    }

    @Test
    void emptyItemsAndDuplicateJournalScheduleNothing() {
        VendorBill empty = bill(VendorBillStatus.APPROVED, BigDecimal.ZERO);
        when(vendorBillRepository.findById(1L)).thenReturn(Optional.of(empty));
        lenient().when(vendorBillItemRepository.findByVendorBillId(1L)).thenReturn(List.of());
        assertThrows(BusinessRuleException.class, () -> service.post(1L));

        VendorBill duplicate = bill(VendorBillStatus.APPROVED, BigDecimal.ZERO);
        VendorBillItem item = item(duplicate);
        duplicate.setItems(new ArrayList<>(List.of(item)));
        when(vendorBillRepository.findById(1L)).thenReturn(Optional.of(duplicate));
        when(vendorBillItemRepository.findByVendorBillId(1L)).thenReturn(List.of(item));
        when(journalEntryRepository.findBySourceTypeAndSourceId(JournalSourceType.VENDOR_BILL, 1L))
                .thenReturn(Optional.of(new JournalEntry()));
        assertThrows(BusinessRuleException.class, () -> service.post(1L));

        verifyNoLifecycleNotification();
    }

    @Test
    void makerCheckerPeriodJournalAndAuditFailuresScheduleNothing() {
        VendorBill bill = postableBill();
        doThrow(new BusinessRuleException("maker-checker"))
                .when(makerCheckerService).validateChecker(55L, "Vendor Bill");
        assertThrows(BusinessRuleException.class, () -> service.post(1L));
        verifyNoLifecycleNotification();

        org.mockito.Mockito.reset(makerCheckerService);
        doThrow(new BusinessRuleException("closed period"))
                .when(accountingPeriodService).validatePostingDate(bill.getPostingDate());
        assertThrows(BusinessRuleException.class, () -> service.post(1L));
        verifyNoLifecycleNotification();

        org.mockito.Mockito.reset(accountingPeriodService);
        doThrow(new IllegalStateException("account update failed"))
                .when(accountRepository).save(any(Account.class));
        assertThrows(IllegalStateException.class, () -> service.post(1L));
        verifyNoLifecycleNotification();

        org.mockito.Mockito.reset(accountRepository);
        doThrow(new IllegalStateException("audit failed"))
                .when(auditLogService).log(
                        AuditAction.POSTED, "VENDOR_BILL", 1L, "APPROVED", "POSTED");
        assertThrows(IllegalStateException.class, () -> service.post(1L));
        verifyNoLifecycleNotification();
    }

    @Test
    void budgetNotificationRemainsSeparateAndCurrentUserOnly() {
        VendorBill bill = postableBill();
        com.nexaerp.budget.dto.BudgetWarningDto warning =
                com.nexaerp.budget.dto.BudgetWarningDto.builder()
                        .budgetId(7L).accountName("Expense")
                        .exceededAmount(new BigDecimal("10.00")).build();
        when(budgetCheckService.checkExpenseAccount(any(), any(), any()))
                .thenReturn(Optional.of(warning));

        service.post(1L);

        verify(notificationService).scheduleForCurrentUserAfterCommit(
                NotificationType.BUDGET_EXCEEDED,
                NotificationPriority.HIGH,
                NotificationModule.BUDGET,
                "Budget exceeded",
                "Budget for Expense exceeded by 10.00.",
                "/budget/7/variance",
                "BUDGET",
                7L
        );
        verify(notificationService).scheduleUniqueForUsersAfterCommit(
                Arrays.asList(55L, 99L),
                NotificationType.VENDOR_BILL_POSTED,
                NotificationPriority.MEDIUM,
                NotificationModule.VENDOR_BILL,
                "Vendor bill posted",
                "Vendor bill BILL-2026-000001 was posted successfully.",
                "/vendor-bill/1", "VENDOR_BILL", 1L
        );
    }

    @Test
    void draftAndApprovedCancellationScheduleCreatorAndActor() {
        for (VendorBillStatus status : List.of(VendorBillStatus.DRAFT, VendorBillStatus.APPROVED)) {
            VendorBill bill = cancellableBill(status);

            service.cancel(1L, VendorBillCancelledReason.VENDOR_REQUESTED);

            assertEquals(VendorBillStatus.CANCELLED, bill.getStatus());
            verifyCancelledNotification();
            org.mockito.Mockito.clearInvocations(notificationService);
        }
    }

    @Test
    void postedCancellationSchedulesOnlyAfterSuccessfulReversal() {
        VendorBill bill = cancellableBill(VendorBillStatus.POSTED);
        JournalEntry original = originalJournal(JournalStatus.POSTED);
        when(journalEntryRepository.findBySourceTypeAndSourceId(JournalSourceType.VENDOR_BILL, 1L))
                .thenReturn(Optional.of(original));
        when(journalLineRepository.findByJournalEntryId(100L))
                .thenReturn(List.of(line(original, expense)));

        service.cancel(1L, VendorBillCancelledReason.WRONG_ENTRY);

        assertEquals(VendorBillStatus.CANCELLED, bill.getStatus());
        assertEquals(JournalStatus.REVERSED, original.getStatus());
        verifyCancelledNotification();
    }

    @Test
    void invalidCancellationPathsScheduleNothing() {
        VendorBill cancelled = cancellableBill(VendorBillStatus.CANCELLED);
        assertThrows(BusinessRuleException.class,
                () -> service.cancel(1L, VendorBillCancelledReason.WRONG_ENTRY));

        VendorBill missingReason = cancellableBill(VendorBillStatus.DRAFT);
        assertThrows(BusinessRuleException.class, () -> service.cancel(1L, null));

        VendorBill partial = cancellableBill(VendorBillStatus.PARTIAL);
        partial.setPaidAmount(BigDecimal.ONE);
        assertThrows(BusinessRuleException.class,
                () -> service.cancel(1L, VendorBillCancelledReason.WRONG_ENTRY));

        verifyNoLifecycleNotification();
    }

    @Test
    void closedPeriodReversedOrMissingJournalBlocksPostedCancellation() {
        VendorBill bill = cancellableBill(VendorBillStatus.POSTED);
        doThrow(new BusinessRuleException("closed period"))
                .when(accountingPeriodService).validatePostingDate(any(LocalDate.class));
        assertThrows(BusinessRuleException.class,
                () -> service.cancel(1L, VendorBillCancelledReason.WRONG_ENTRY));
        verifyNoLifecycleNotification();

        org.mockito.Mockito.reset(accountingPeriodService);
        when(journalEntryRepository.findBySourceTypeAndSourceId(JournalSourceType.VENDOR_BILL, 1L))
                .thenReturn(Optional.of(originalJournal(JournalStatus.REVERSED)));
        assertThrows(BusinessRuleException.class,
                () -> service.cancel(1L, VendorBillCancelledReason.WRONG_ENTRY));
        verifyNoLifecycleNotification();

        when(journalEntryRepository.findBySourceTypeAndSourceId(JournalSourceType.VENDOR_BILL, 1L))
                .thenReturn(Optional.empty());
        assertThrows(BusinessRuleException.class,
                () -> service.cancel(1L, VendorBillCancelledReason.WRONG_ENTRY));
        assertEquals(VendorBillStatus.POSTED, bill.getStatus());
        verifyNoLifecycleNotification();
    }

    private VendorBill postableBill() {
        VendorBill bill = bill(VendorBillStatus.APPROVED, BigDecimal.ZERO);
        VendorBillItem item = item(bill);
        bill.setItems(new ArrayList<>(List.of(item)));
        bill.setSubTotal(new BigDecimal("100.00"));
        bill.setDiscountAmount(BigDecimal.ZERO);
        bill.setVatAmount(new BigDecimal("10.00"));
        bill.setTdsAmount(new BigDecimal("5.00"));
        bill.setGrandTotal(new BigDecimal("110.00"));
        bill.setNetPayable(new BigDecimal("105.00"));
        bill.setDueAmount(new BigDecimal("105.00"));
        when(vendorBillRepository.findById(1L)).thenReturn(Optional.of(bill));
        when(vendorBillItemRepository.findByVendorBillId(1L)).thenReturn(List.of(item));
        when(journalEntryRepository.findBySourceTypeAndSourceId(JournalSourceType.VENDOR_BILL, 1L))
                .thenReturn(Optional.empty());
        return bill;
    }

    private VendorBill cancellableBill(VendorBillStatus status) {
        VendorBill bill = bill(status, BigDecimal.ZERO);
        when(vendorBillRepository.findById(1L)).thenReturn(Optional.of(bill));
        lenient().when(vendorBillItemRepository.findByVendorBillId(1L)).thenReturn(List.of());
        return bill;
    }

    private VendorBill bill(VendorBillStatus status, BigDecimal paidAmount) {
        VendorBill bill = VendorBill.builder()
                .id(1L)
                .billNumber("BILL-2026-000001")
                .billDate(LocalDate.of(2026, 7, 10))
                .postingDate(LocalDate.of(2026, 7, 15))
                .party(Party.builder().id(5L).name("Vendor").type(PartyType.VENDOR).build())
                .billType(VendorBillType.EXPENSE)
                .status(status)
                .paidAmount(paidAmount)
                .dueAmount(new BigDecimal("105.00"))
                .netPayable(new BigDecimal("105.00"))
                .items(new ArrayList<>())
                .build();
        bill.setCreatedBy(55L);
        return bill;
    }

    private VendorBillItem item(VendorBill bill) {
        return VendorBillItem.builder()
                .id(10L).vendorBill(bill).expenseAccount(expense)
                .description("Expense").quantity(BigDecimal.ONE)
                .unitPrice(new BigDecimal("100.00"))
                .subTotal(new BigDecimal("100.00"))
                .discountAmount(BigDecimal.ZERO)
                .vatAmount(new BigDecimal("10.00"))
                .tdsAmount(new BigDecimal("5.00"))
                .lineTotal(new BigDecimal("110.00"))
                .build();
    }

    private JournalEntry originalJournal(JournalStatus status) {
        return JournalEntry.builder()
                .id(100L).entryNumber("JE-0001").referenceNumber("BILL-2026-000001")
                .status(status).totalAmount(new BigDecimal("110.00")).build();
    }

    private JournalLine line(JournalEntry journal, Account account) {
        JournalLine line = new JournalLine();
        line.setJournalEntry(journal);
        line.setAccount(account);
        line.setDebit(new BigDecimal("100.00"));
        line.setCredit(BigDecimal.ZERO);
        line.setDescription("Expense");
        return line;
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
                NotificationType.VENDOR_BILL_CANCELLED,
                NotificationPriority.HIGH,
                NotificationModule.VENDOR_BILL,
                "Vendor bill cancelled",
                "Vendor bill BILL-2026-000001 was cancelled.",
                "/vendor-bill/1", "VENDOR_BILL", 1L
        );
    }

    private void verifyNoLifecycleNotification() {
        verify(notificationService, never()).scheduleUniqueForUsersAfterCommit(
                any(), any(), any(), any(), any(), any(), any(), any(), any());
    }
}
