package com.nexaerp.vendorbill;

import com.nexaerp.account.Account;
import com.nexaerp.account.AccountRepository;
import com.nexaerp.account.AccountType;
import com.nexaerp.accountingperiod.AccountingPeriodService;
import com.nexaerp.audit.AuditLogService;
import com.nexaerp.budget.BudgetCheckService;
import com.nexaerp.budget.dto.BudgetWarningDto;
import com.nexaerp.email.BudgetAlertEmailService;
import com.nexaerp.costcenter.CostCenter;
import com.nexaerp.costcenter.CostCenterService;
import com.nexaerp.journal.JournalEntry;
import com.nexaerp.journal.JournalEntryRepository;
import com.nexaerp.journal.JournalLine;
import com.nexaerp.journal.JournalLineRepository;
import com.nexaerp.journal.JournalSourceType;
import com.nexaerp.notification.NotificationService;
import com.nexaerp.notification.NotificationType;
import com.nexaerp.party.Party;
import com.nexaerp.party.PartyRepository;
import com.nexaerp.party.PartyType;
import com.nexaerp.security.CurrentUserService;
import com.nexaerp.security.MakerCheckerService;
import com.nexaerp.settings.SettingKey;
import com.nexaerp.settings.SystemSettingsService;
import com.nexaerp.vendorbill.dto.VendorBillResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorBillServiceImplTest {

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

    @InjectMocks private VendorBillServiceImpl service;

    private final LocalDate postingDate = LocalDate.of(2026, 7, 15);
    private Account expenseAccount;
    private Account payableAccount;
    private Account inputVatAccount;
    private Account tdsPayableAccount;

    @BeforeEach
    void setUp() {
        expenseAccount = account(10L, "5100", "Office Expense", AccountType.EXPENSE);
        payableAccount = account(20L, "2100", "Accounts Payable", AccountType.LIABILITY);
        inputVatAccount = account(30L, "1300", "Input VAT", AccountType.ASSET);
        tdsPayableAccount = account(40L, "2200", "TDS Payable", AccountType.LIABILITY);

        when(systemSettingsService.getAccount(SettingKey.DEFAULT_PAYABLE_ACCOUNT))
                .thenReturn(payableAccount);
        when(systemSettingsService.getAccount(SettingKey.DEFAULT_INPUT_VAT))
                .thenReturn(inputVatAccount);
        when(systemSettingsService.getAccount(SettingKey.DEFAULT_TDS_PAYABLE))
                .thenReturn(tdsPayableAccount);
        when(currentUserService.getCurrentUserId()).thenReturn(99L);
        when(journalEntryRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(invocation -> {
            JournalEntry journal = invocation.getArgument(0);
            journal.setId(100L);
            return journal;
        });
        when(vendorBillRepository.save(any(VendorBill.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(budgetCheckService.checkExpenseAccount(any(), any(), any()))
                .thenReturn(Optional.empty());
    }

    @Test
    void postingInsideBudgetSucceedsAndReturnsEmptyWarnings() {
        VendorBill bill = billWithItems(List.of(
                item(1L, expenseAccount, "100.00", "10.00", "13.50", "4.50")
        ));

        VendorBillResponseDto response = service.post(bill.getId());

        assertEquals(VendorBillStatus.POSTED, response.getStatus());
        assertTrue(response.getBudgetWarnings().isEmpty());
        verify(budgetCheckService).checkExpenseAccount(
                expenseAccount, postingDate, new BigDecimal("90.00"));
        verify(notificationService, never()).createForCurrentUser(
                any(), any(), any(), any(), any(), any());
    }

    @Test
    void postingOutsideBudgetReturnsWarningCreatesNotificationAndKeepsAccountingAmounts() {
        VendorBill bill = billWithItems(List.of(
                item(1L, expenseAccount, "100.00", "10.00", "13.50", "4.50")
        ));
        BudgetWarningDto warning = warning(expenseAccount, "15.00");
        when(budgetCheckService.checkExpenseAccount(
                expenseAccount, postingDate, new BigDecimal("90.00")))
                .thenReturn(Optional.of(warning));

        VendorBillResponseDto response = service.post(bill.getId());

        assertEquals(VendorBillStatus.POSTED, response.getStatus());
        assertEquals(List.of(warning), response.getBudgetWarnings());
        verify(budgetAlertEmailService).scheduleAfterCommit(
                "Vendor Bill",
                bill.getId(),
                bill.getBillNumber(),
                postingDate,
                List.of(warning)
        );
        verify(notificationService).createForCurrentUser(
                NotificationType.BUDGET_EXCEEDED,
                "Budget exceeded",
                "Budget for Office Expense exceeded by 15.00.",
                "/budget/7/variance",
                "BUDGET",
                7L
        );

        ArgumentCaptor<JournalLine> lineCaptor = ArgumentCaptor.forClass(JournalLine.class);
        verify(journalLineRepository, times(4)).save(lineCaptor.capture());
        List<JournalLine> lines = lineCaptor.getAllValues();

        assertLine(lines.get(0), expenseAccount, "90.00", "0");
        assertLine(lines.get(1), inputVatAccount, "13.50", "0");
        assertLine(lines.get(2), payableAccount, "0", "99.00");
        assertLine(lines.get(3), tdsPayableAccount, "0", "4.50");
    }

    @Test
    void multipleItemsForSameAccountAreAggregatedIntoOneBudgetCheck() {
        VendorBill bill = billWithItems(List.of(
                item(1L, expenseAccount, "100.00", "10.00", "0", "0"),
                item(2L, expenseAccount, "60.00", "5.00", "0", "0")
        ));

        VendorBillResponseDto response = service.post(bill.getId());

        assertEquals(VendorBillStatus.POSTED, response.getStatus());
        verify(budgetCheckService, times(1)).checkExpenseAccount(
                expenseAccount, postingDate, new BigDecimal("145.00"));
    }

    @Test
    void multipleExpenseAccountsAreCheckedSeparately() {
        Account travelAccount = account(11L, "5200", "Travel Expense", AccountType.EXPENSE);
        VendorBill bill = billWithItems(List.of(
                item(1L, expenseAccount, "100.00", "10.00", "0", "0"),
                item(2L, travelAccount, "60.00", "5.00", "0", "0")
        ));

        service.post(bill.getId());

        verify(budgetCheckService).checkExpenseAccount(
                expenseAccount, postingDate, new BigDecimal("90.00"));
        verify(budgetCheckService).checkExpenseAccount(
                travelAccount, postingDate, new BigDecimal("55.00"));
        verify(budgetAlertEmailService, times(1)).scheduleAfterCommit(
                eq("Vendor Bill"),
                eq(bill.getId()),
                eq(bill.getBillNumber()),
                eq(postingDate),
                any()
        );
    }

    @Test
    void noActiveBudgetProducesNoWarningAndDoesNotBlockPosting() {
        VendorBill bill = billWithItems(List.of(
                item(1L, expenseAccount, "75.00", "0", "0", "0")
        ));
        when(budgetCheckService.checkExpenseAccount(
                expenseAccount, postingDate, new BigDecimal("75.00")))
                .thenReturn(Optional.empty());

        VendorBillResponseDto response = service.post(bill.getId());

        assertEquals(VendorBillStatus.POSTED, response.getStatus());
        assertTrue(response.getBudgetWarnings().isEmpty());
        verify(journalEntryRepository).save(any(JournalEntry.class));
        verify(budgetAlertEmailService).scheduleAfterCommit(
                "Vendor Bill",
                bill.getId(),
                bill.getBillNumber(),
                postingDate,
                List.of()
        );
    }

    @Test
    void itemCostCenterPropagatesOnlyToExpenseDebitLine() {
        CostCenter costCenter = CostCenter.builder()
                .id(8L).code("OPS").name("Operations").isActive(true).build();
        VendorBillItem item = item(1L, expenseAccount, "100.00", "0", "10.00", "5.00");
        item.setCostCenter(costCenter);
        when(costCenterService.resolveActive(costCenter.getId())).thenReturn(costCenter);
        billWithItems(List.of(item));

        service.post(1L);

        ArgumentCaptor<JournalLine> lineCaptor = ArgumentCaptor.forClass(JournalLine.class);
        verify(journalLineRepository, times(4)).save(lineCaptor.capture());
        List<JournalLine> lines = lineCaptor.getAllValues();
        assertSame(costCenter, lines.get(0).getCostCenter());
        assertTrue(lines.subList(1, lines.size()).stream().allMatch(line -> line.getCostCenter() == null));
    }

    private VendorBill billWithItems(List<VendorBillItem> items) {
        VendorBill bill = VendorBill.builder()
                .id(1L)
                .billNumber("BILL-2026-000001")
                .billDate(LocalDate.of(2026, 7, 10))
                .postingDate(postingDate)
                .party(Party.builder()
                        .id(5L)
                        .code("V-001")
                        .name("Test Vendor")
                        .type(PartyType.VENDOR)
                        .isActive(true)
                        .phone("0123456789")
                        .build())
                .status(VendorBillStatus.APPROVED)
                .subTotal(sum(items, VendorBillItem::getSubTotal))
                .discountAmount(sum(items, VendorBillItem::getDiscountAmount))
                .vatAmount(sum(items, VendorBillItem::getVatAmount))
                .tdsAmount(sum(items, VendorBillItem::getTdsAmount))
                .grandTotal(sum(items, VendorBillItem::getLineTotal))
                .netPayable(sum(items, item -> item.getLineTotal().subtract(item.getTdsAmount())))
                .paidAmount(BigDecimal.ZERO)
                .dueAmount(sum(items, item -> item.getLineTotal().subtract(item.getTdsAmount())))
                .items(new ArrayList<>(items))
                .build();
        bill.setCreatedBy(55L);
        items.forEach(item -> item.setVendorBill(bill));

        when(vendorBillRepository.findById(bill.getId())).thenReturn(Optional.of(bill));
        when(vendorBillItemRepository.findByVendorBillId(bill.getId())).thenReturn(items);
        when(journalEntryRepository.findBySourceTypeAndSourceId(
                JournalSourceType.VENDOR_BILL, bill.getId())).thenReturn(Optional.empty());
        return bill;
    }

    private VendorBillItem item(
            Long id,
            Account account,
            String subTotal,
            String discount,
            String vat,
            String tds
    ) {
        BigDecimal subTotalAmount = new BigDecimal(subTotal);
        BigDecimal discountAmount = new BigDecimal(discount);
        BigDecimal vatAmount = new BigDecimal(vat);
        BigDecimal tdsAmount = new BigDecimal(tds);
        return VendorBillItem.builder()
                .id(id)
                .expenseAccount(account)
                .description("Item " + id)
                .quantity(BigDecimal.ONE)
                .unitPrice(subTotalAmount)
                .discountPercent(BigDecimal.ZERO)
                .discountAmount(discountAmount)
                .vatRate(BigDecimal.ZERO)
                .vatAmount(vatAmount)
                .tdsRate(BigDecimal.ZERO)
                .tdsAmount(tdsAmount)
                .subTotal(subTotalAmount)
                .lineTotal(subTotalAmount.subtract(discountAmount).add(vatAmount))
                .build();
    }

    private Account account(Long id, String code, String name, AccountType type) {
        Account account = new Account();
        account.setId(id);
        account.setCode(code);
        account.setName(name);
        account.setType(type);
        account.setIsActive(true);
        account.setCurrentBalance(BigDecimal.ZERO);
        return account;
    }

    private BudgetWarningDto warning(Account account, String exceededAmount) {
        return BudgetWarningDto.builder()
                .budgetId(7L)
                .accountId(account.getId())
                .accountCode(account.getCode())
                .accountName(account.getName())
                .exceededAmount(new BigDecimal(exceededAmount))
                .message("Budget exceeded")
                .build();
    }

    private BigDecimal sum(
            List<VendorBillItem> items,
            java.util.function.Function<VendorBillItem, BigDecimal> mapper
    ) {
        return items.stream().map(mapper).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void assertLine(
            JournalLine line,
            Account expectedAccount,
            String expectedDebit,
            String expectedCredit
    ) {
        assertSame(expectedAccount, line.getAccount());
        assertEquals(0, new BigDecimal(expectedDebit).compareTo(line.getDebit()));
        assertEquals(0, new BigDecimal(expectedCredit).compareTo(line.getCredit()));
    }
}
