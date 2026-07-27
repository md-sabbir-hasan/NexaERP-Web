package com.nexaerp.report;

import com.nexaerp.account.Account;
import com.nexaerp.costcenter.CostCenter;
import com.nexaerp.costcenter.CostCenterRepository;
import com.nexaerp.journal.JournalEntry;
import com.nexaerp.journal.JournalLine;
import com.nexaerp.journal.JournalLineRepository;
import com.nexaerp.journal.JournalSourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CostCenterTransactionReportTest {

    @Mock private com.nexaerp.account.AccountRepository accountRepository;
    @Mock private JournalLineRepository journalLineRepository;
    @Mock private com.nexaerp.invoice.InvoiceRepository invoiceRepository;
    @Mock private com.nexaerp.vendorbill.VendorBillRepository vendorBillRepository;
    @Mock private com.nexaerp.payment.PaymentRepository paymentRepository;
    @Mock private com.nexaerp.party.PartyRepository partyRepository;
    @Mock private CashFlowStatementService cashFlowStatementService;
    @Mock private BudgetVsActualReportService budgetVsActualReportService;
    @Mock private CostCenterRepository costCenterRepository;
    @InjectMocks private ReportServiceImpl service;

    @Test
    void reportReturnsRowsAndTotalsForSelectedCostCenter() {
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 31);
        CostCenter costCenter = CostCenter.builder().id(1L).code("OPS").name("Operations").isActive(false).build();
        Account account = new Account();
        account.setCode("5100");
        account.setName("Office Expense");
        JournalEntry entry = JournalEntry.builder().id(10L).entryNumber("JE-0010").date(from)
                .sourceType(JournalSourceType.MANUAL).sourceId(null).build();
        JournalLine debit = JournalLine.builder().journalEntry(entry).account(account).costCenter(costCenter)
                .debit(new BigDecimal("125.00")).credit(BigDecimal.ZERO).description("Office supplies").build();
        when(costCenterRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(costCenter));
        when(journalLineRepository.findCostCenterTransactions(
                org.mockito.ArgumentMatchers.eq(1L), anyList(),
                org.mockito.ArgumentMatchers.eq(from), org.mockito.ArgumentMatchers.eq(to)))
                .thenReturn(List.of(debit));

        var report = service.getCostCenterTransactions(1L, from, to);

        assertEquals(1, report.getRows().size());
        assertEquals(new BigDecimal("125.00"), report.getTotalDebit());
        assertEquals(BigDecimal.ZERO, report.getTotalCredit());
        assertEquals(new BigDecimal("125.00"), report.getNetAmount());
        assertEquals("JE-0010", report.getRows().get(0).getJournalNumber());
    }

    @Test
    void invalidDateRangeIsRejected() {
        assertThrows(RuntimeException.class, () -> service.getCostCenterTransactions(
                1L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 7, 1)));
    }
}
