package com.nexaerp.globalsearch;

import com.nexaerp.account.Account;
import com.nexaerp.account.AccountRepository;
import com.nexaerp.account.AccountType;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.globalsearch.dto.GlobalSearchResultDto;
import com.nexaerp.invoice.Invoice;
import com.nexaerp.invoice.InvoiceRepository;
import com.nexaerp.invoice.InvoiceStatus;
import com.nexaerp.journal.JournalEntry;
import com.nexaerp.journal.JournalEntryRepository;
import com.nexaerp.journal.JournalEntryType;
import com.nexaerp.journal.JournalStatus;
import com.nexaerp.party.Party;
import com.nexaerp.party.PartyRepository;
import com.nexaerp.party.PartyType;
import com.nexaerp.payment.Payment;
import com.nexaerp.payment.PaymentRepository;
import com.nexaerp.payment.PaymentStatus;
import com.nexaerp.vendorbill.VendorBill;
import com.nexaerp.vendorbill.VendorBillRepository;
import com.nexaerp.vendorbill.VendorBillStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalSearchServiceImplTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private VendorBillRepository vendorBillRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PartyRepository partyRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private JournalEntryRepository journalEntryRepository;

    private GlobalSearchServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new GlobalSearchServiceImpl(
                invoiceRepository,
                vendorBillRepository,
                paymentRepository,
                partyRepository,
                accountRepository,
                journalEntryRepository);
    }

    @Test
    void mapsEverySupportedModuleInDeterministicOrder() {
        Party party = party(7L, "P-007", "Acme", PartyType.BOTH, true);
        when(invoiceRepository.findByInvoiceNumberContainingIgnoreCase(eq("00"), any()))
                .thenReturn(new PageImpl<>(List.of(Invoice.builder().id(1L)
                        .invoiceNumber("INV-001").party(party).status(InvoiceStatus.POSTED).build())));
        when(vendorBillRepository.findByBillNumberContainingIgnoreCase(eq("00"), any()))
                .thenReturn(new PageImpl<>(List.of(VendorBill.builder().id(2L)
                        .billNumber("BILL-002").party(party).status(VendorBillStatus.APPROVED).build())));
        when(paymentRepository.findByPaymentNumberContainingIgnoreCase(eq("00"), any()))
                .thenReturn(new PageImpl<>(List.of(Payment.builder().id(3L)
                        .paymentNumber("PAY-003").party(party).status(PaymentStatus.DRAFT).build())));
        when(partyRepository.findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(
                eq("00"), eq("00"), any()))
                .thenReturn(new PageImpl<>(List.of(party)));

        Account account = new Account();
        account.setId(4L);
        account.setCode("1000");
        account.setName("Cash");
        account.setType(AccountType.ASSET);
        account.setIsActive(true);
        when(accountRepository.findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(
                eq("00"), eq("00"), any()))
                .thenReturn(new PageImpl<>(List.of(account)));
        when(journalEntryRepository.findByEntryNumberContainingIgnoreCase(eq("00"), any()))
                .thenReturn(new PageImpl<>(List.of(JournalEntry.builder().id(5L)
                        .entryNumber("JE-005").type(JournalEntryType.GENERAL)
                        .status(JournalStatus.POSTED).build())));

        var response = service.search(" 00 ", 5, authentication(
                "VIEW_INVOICE", "VIEW_VENDOR_BILL", "VIEW_PAYMENT",
                "VIEW_PARTY", "VIEW_ACCOUNTS", "VIEW_JOURNAL"));

        assertThat(response.getQuery()).isEqualTo("00");
        assertThat(response.getGroups()).extracting(group -> group.getType())
                .containsExactly(
                        GlobalSearchResultType.INVOICE,
                        GlobalSearchResultType.VENDOR_BILL,
                        GlobalSearchResultType.PAYMENT,
                        GlobalSearchResultType.PARTY,
                        GlobalSearchResultType.ACCOUNT,
                        GlobalSearchResultType.JOURNAL_ENTRY);
        assertThat(response.getGroups().get(0).getResults().get(0))
                .extracting(GlobalSearchResultDto::getTitle,
                        GlobalSearchResultDto::getSubtitle,
                        GlobalSearchResultDto::getStatus)
                .containsExactly("INV-001", "Acme", "POSTED");
        assertThat(response.getGroups().get(3).getResults().get(0).getSubtitle())
                .isEqualTo("P-007 · BOTH");
        assertThat(response.getGroups().get(4).getResults().get(0).getTitle())
                .isEqualTo("1000 - Cash");
    }

    @Test
    void mixedPermissionsOnlyQueryAndReturnAuthorizedModules() {
        when(invoiceRepository.findByInvoiceNumberContainingIgnoreCase(eq("inv"), any()))
                .thenReturn(new PageImpl<>(List.of(Invoice.builder().id(1L)
                        .invoiceNumber("INV-1").status(InvoiceStatus.DRAFT).build())));
        when(accountRepository.findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(
                eq("inv"), eq("inv"), any())).thenReturn(new PageImpl<>(List.of()));

        var response = service.search("inv", 5,
                authentication("VIEW_INVOICE", "VIEW_ACCOUNTS", "LOOKUP_PARTIES"));

        assertThat(response.getGroups()).extracting(group -> group.getType())
                .containsExactly(GlobalSearchResultType.INVOICE);
        verify(vendorBillRepository, never()).findByBillNumberContainingIgnoreCase(any(), any());
        verify(paymentRepository, never()).findByPaymentNumberContainingIgnoreCase(any(), any());
        verify(partyRepository, never())
                .findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(any(), any(), any());
        verify(journalEntryRepository, never()).findByEntryNumberContainingIgnoreCase(any(), any());
    }

    @Test
    void noRelevantPermissionsReturnsSuccessfulEmptyGroupsWithoutRepositories() {
        var response = service.search("cash", 5, authentication("MANAGE_USERS"));

        assertThat(response.getGroups()).isEmpty();
        verify(invoiceRepository, never()).findByInvoiceNumberContainingIgnoreCase(any(), any());
        verify(accountRepository, never())
                .findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(any(), any(), any());
    }

    @Test
    void validatesMinimumAndMaximumQueryLength() {
        assertThatThrownBy(() -> service.search(" ", 5, authentication("VIEW_INVOICE")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("at least 2");
        assertThatThrownBy(() -> service.search("x".repeat(101), 5,
                authentication("VIEW_INVOICE")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("100");
    }

    @Test
    void defaultsAndCapsPageableLimit() {
        when(invoiceRepository.findByInvoiceNumberContainingIgnoreCase(any(), any()))
                .thenReturn(new PageImpl<>(List.of()));
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);

        service.search("inv", null, authentication("VIEW_INVOICE"));
        service.search("inv", 999, authentication("VIEW_INVOICE"));
        verify(invoiceRepository, times(2))
                .findByInvoiceNumberContainingIgnoreCase(eq("inv"), pageable.capture());
        assertThat(pageable.getAllValues())
                .extracting(Pageable::getPageSize)
                .containsExactly(5, 10);
    }

    @Test
    void delegatesPartyAndAccountCodeAndNameSearchWithBoundedPageable() {
        when(partyRepository.findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(
                any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(accountRepository.findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(
                any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        service.search("cash", 7, authentication("VIEW_PARTY", "VIEW_ACCOUNTS"));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(partyRepository).findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(
                eq("cash"), eq("cash"), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(7);
        verify(accountRepository).findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(
                eq("cash"), eq("cash"), any(Pageable.class));
    }

    @Test
    void resultDtoContractContainsOnlySafeFields() {
        Set<String> fields = Arrays.stream(GlobalSearchResultDto.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());

        assertThat(fields).containsExactlyInAnyOrder("id", "type", "title", "subtitle", "status");
        assertThat(fields).doesNotContain(
                "url", "route", "notes", "amount", "address", "taxId",
                "bankAccount", "identity", "fileUrl", "lines");
    }

    private Authentication authentication(String... authorities) {
        return new UsernamePasswordAuthenticationToken(
                "user@example.com",
                "unused",
                Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList());
    }

    private Party party(Long id, String code, String name, PartyType type, boolean active) {
        return Party.builder()
                .id(id)
                .code(code)
                .name(name)
                .type(type)
                .isActive(active)
                .build();
    }
}
