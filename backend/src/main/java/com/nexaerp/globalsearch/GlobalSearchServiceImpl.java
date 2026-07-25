package com.nexaerp.globalsearch;

import com.nexaerp.account.Account;
import com.nexaerp.account.AccountRepository;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.globalsearch.dto.GlobalSearchGroupDto;
import com.nexaerp.globalsearch.dto.GlobalSearchResponseDto;
import com.nexaerp.globalsearch.dto.GlobalSearchResultDto;
import com.nexaerp.invoice.Invoice;
import com.nexaerp.invoice.InvoiceRepository;
import com.nexaerp.journal.JournalEntry;
import com.nexaerp.journal.JournalEntryRepository;
import com.nexaerp.party.Party;
import com.nexaerp.party.PartyRepository;
import com.nexaerp.payment.Payment;
import com.nexaerp.payment.PaymentRepository;
import com.nexaerp.vendorbill.VendorBill;
import com.nexaerp.vendorbill.VendorBillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GlobalSearchServiceImpl implements GlobalSearchService {

    static final int DEFAULT_LIMIT = 5;
    static final int MAX_LIMIT = 10;
    static final int MAX_QUERY_LENGTH = 100;

    private final InvoiceRepository invoiceRepository;
    private final VendorBillRepository vendorBillRepository;
    private final PaymentRepository paymentRepository;
    private final PartyRepository partyRepository;
    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;

    @Override
    @Transactional(readOnly = true)
    public GlobalSearchResponseDto search(
            String query,
            Integer requestedLimit,
            Authentication authentication) {
        String normalizedQuery = normalizeQuery(query);
        int limit = normalizeLimit(requestedLimit);
        Pageable pageable = PageRequest.of(0, limit);
        Set<String> authorities = authoritiesOf(authentication);
        List<GlobalSearchGroupDto> groups = new ArrayList<>();

        if (authorities.contains("VIEW_INVOICE")) {
            addGroup(groups, GlobalSearchResultType.INVOICE,
                    invoiceRepository
                            .findByInvoiceNumberContainingIgnoreCase(normalizedQuery, pageable)
                            .getContent().stream().map(this::mapInvoice).toList());
        }
        if (authorities.contains("VIEW_VENDOR_BILL")) {
            addGroup(groups, GlobalSearchResultType.VENDOR_BILL,
                    vendorBillRepository
                            .findByBillNumberContainingIgnoreCase(normalizedQuery, pageable)
                            .getContent().stream().map(this::mapVendorBill).toList());
        }
        if (authorities.contains("VIEW_PAYMENT")) {
            addGroup(groups, GlobalSearchResultType.PAYMENT,
                    paymentRepository
                            .findByPaymentNumberContainingIgnoreCase(normalizedQuery, pageable)
                            .getContent().stream().map(this::mapPayment).toList());
        }
        if (authorities.contains("VIEW_PARTY")) {
            addGroup(groups, GlobalSearchResultType.PARTY,
                    partyRepository
                            .findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(
                                    normalizedQuery, normalizedQuery, pageable)
                            .getContent().stream().map(this::mapParty).toList());
        }
        if (authorities.contains("VIEW_ACCOUNTS")) {
            addGroup(groups, GlobalSearchResultType.ACCOUNT,
                    accountRepository
                            .findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(
                                    normalizedQuery, normalizedQuery, pageable)
                            .getContent().stream().map(this::mapAccount).toList());
        }
        if (authorities.contains("VIEW_JOURNAL")) {
            addGroup(groups, GlobalSearchResultType.JOURNAL_ENTRY,
                    journalEntryRepository
                            .findByEntryNumberContainingIgnoreCase(normalizedQuery, pageable)
                            .getContent().stream().map(this::mapJournal).toList());
        }

        return GlobalSearchResponseDto.builder()
                .query(normalizedQuery)
                .groups(groups)
                .build();
    }

    private String normalizeQuery(String query) {
        String normalized = query == null ? "" : query.trim();
        if (normalized.length() < 2) {
            throw new BusinessRuleException("Search query must contain at least 2 characters");
        }
        if (normalized.length() > MAX_QUERY_LENGTH) {
            throw new BusinessRuleException(
                    "Search query must not exceed " + MAX_QUERY_LENGTH + " characters");
        }
        return normalized;
    }

    private int normalizeLimit(Integer requestedLimit) {
        if (requestedLimit == null || requestedLimit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(requestedLimit, MAX_LIMIT);
    }

    private Set<String> authoritiesOf(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Set.of();
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    private void addGroup(
            List<GlobalSearchGroupDto> groups,
            GlobalSearchResultType type,
            List<GlobalSearchResultDto> results) {
        if (!results.isEmpty()) {
            groups.add(GlobalSearchGroupDto.builder().type(type).results(results).build());
        }
    }

    private GlobalSearchResultDto mapInvoice(Invoice invoice) {
        return result(invoice.getId(), GlobalSearchResultType.INVOICE,
                invoice.getInvoiceNumber(), partyName(invoice.getParty()),
                enumName(invoice.getStatus()));
    }

    private GlobalSearchResultDto mapVendorBill(VendorBill bill) {
        return result(bill.getId(), GlobalSearchResultType.VENDOR_BILL,
                bill.getBillNumber(), partyName(bill.getParty()), enumName(bill.getStatus()));
    }

    private GlobalSearchResultDto mapPayment(Payment payment) {
        return result(payment.getId(), GlobalSearchResultType.PAYMENT,
                payment.getPaymentNumber(), partyName(payment.getParty()),
                enumName(payment.getStatus()));
    }

    private GlobalSearchResultDto mapParty(Party party) {
        String subtitle = String.join(" · ",
                safeText(party.getCode(), "No code"),
                enumName(party.getType()));
        return result(party.getId(), GlobalSearchResultType.PARTY,
                party.getName(), subtitle, Boolean.TRUE.equals(party.getIsActive())
                        ? "ACTIVE" : "INACTIVE");
    }

    private GlobalSearchResultDto mapAccount(Account account) {
        String title = safeText(account.getCode(), "") + " - " + safeText(account.getName(), "");
        return result(account.getId(), GlobalSearchResultType.ACCOUNT,
                title.trim(), enumName(account.getType()),
                Boolean.TRUE.equals(account.getIsActive()) ? "ACTIVE" : "INACTIVE");
    }

    private GlobalSearchResultDto mapJournal(JournalEntry journal) {
        return result(journal.getId(), GlobalSearchResultType.JOURNAL_ENTRY,
                journal.getEntryNumber(), enumName(journal.getType()), enumName(journal.getStatus()));
    }

    private GlobalSearchResultDto result(
            Long id,
            GlobalSearchResultType type,
            String title,
            String subtitle,
            String status) {
        return GlobalSearchResultDto.builder()
                .id(id)
                .type(type)
                .title(safeText(title, "Untitled"))
                .subtitle(safeText(subtitle, type.name().replace('_', ' ')))
                .status(safeText(status, "UNKNOWN"))
                .build();
    }

    private String partyName(Party party) {
        return party == null ? "Party unavailable" : safeText(party.getName(), "Party unavailable");
    }

    private String enumName(Enum<?> value) {
        return value == null ? "" : value.name();
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
