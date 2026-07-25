package com.nexaerp.party;

import com.nexaerp.account.Account;
import com.nexaerp.account.AccountRepository;
import com.nexaerp.audit.AuditAction;
import com.nexaerp.audit.AuditLogService;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.journal.*;
import com.nexaerp.party.dto.PartyRequestDto;
import com.nexaerp.party.dto.PartyResponseDto;
import com.nexaerp.settings.SettingKey;
import com.nexaerp.settings.SystemSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class partyServiceImpl implements PartyService{

    private final PartyRepository partyRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalLineRepository journalLineRepository;
    private final AccountRepository accountRepository;
    private final SystemSettingsService systemSettingsService;
    private final AuditLogService auditLogService;


    @Override
    @Transactional
    public PartyResponseDto create(PartyRequestDto request) {
        if (partyRepository.existsByPhone(request.getPhone())) {
            throw new BusinessRuleException("Phone already exists: " + request.getPhone());
        }

        Party party = new Party();

        mapRequestToParty(request, party);
        party.setCode(generateCode());
        party.setIsActive(true);

        Party saved = partyRepository.save(party);

        if (request.getOpeningBalance() != null
                && request.getOpeningBalance().compareTo(BigDecimal.ZERO) != 0) {
            createOpeningBalanceEntry(saved);
        }

        auditLogService.log(
                AuditAction.CREATED,
                "PARTY",
                saved.getId(),
                null,
                "Party created: " + saved.getCode() + " - " + saved.getName()
        );

        return toResponse(saved);
    }

    @Override
    @Transactional
    public PartyResponseDto update(Long id, PartyRequestDto request) {
        Party party = partyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Party not found"));

        String oldValue = "Party before update: " + party.getCode() + " - " + party.getName();

        mapRequestToParty(request, party);

        Party saved = partyRepository.save(party);

        auditLogService.log(
                AuditAction.UPDATED,
                "PARTY",
                saved.getId(),
                oldValue,
                "Party updated: " + saved.getCode() + " - " + saved.getName()
        );

        return toResponse(saved);
    }

    @Override
    public PartyResponseDto getById(Long id) {
        Party party = partyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Party not found"));
        return toResponse(party);
    }

    @Override
    public List<PartyResponseDto> getAll() {
        return partyRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PartyResponseDto> getByType(PartyType type) {
        // BOTH type will  do include
        return partyRepository.findByTypeOrType(type, PartyType.BOTH)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        Party party = partyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Party not found"));

        if (Boolean.FALSE.equals(party.getIsActive())) {
            throw new BusinessRuleException("Party is already inactive");
        }

        party.setIsActive(false);

        Party saved = partyRepository.save(party);

        auditLogService.log(
                AuditAction.DEACTIVATED,
                "PARTY",
                saved.getId(),
                "ACTIVE",
                "INACTIVE"
        );
    }

    @Override
    @Transactional
    public void activate(Long id) {
        Party party = partyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Party not found"));

        if (Boolean.TRUE.equals(party.getIsActive())) {
            throw new BusinessRuleException("Party is already active");
        }

        party.setIsActive(true);

        partyRepository.save(party);

        auditLogService.log(
                AuditAction.ACTIVATED,
                "PARTY",
                party.getId(),
                "INACTIVE",
                "ACTIVE"
        );
    }


                              // -- Private Helpers --
    private void createOpeningBalanceEntry (Party party){
        // Find Account
        Account partyAccount = (party.getType() == PartyType.VENDOR)
                ? systemSettingsService.getAccount(SettingKey.DEFAULT_PAYABLE_ACCOUNT)
                : systemSettingsService.getAccount(SettingKey.DEFAULT_RECEIVABLE_ACCOUNT);

        Account openingEquity = systemSettingsService.getAccount(
                SettingKey.DEFAULT_OPENING_EQUITY);

        // Make Journal Entry

        JournalEntry entry = new JournalEntry();
        entry.setEntryNumber(generateEntryNumber());
        entry.setDate(LocalDate.now());
        entry.setDescription("Opening Balance - " + party.getName());
        entry.setType(JournalEntryType.GENERAL);
        entry.setStatus(JournalStatus.POSTED);
        entry.setSourceType(JournalSourceType.MANUAL);
        entry.setTotalAmount(party.getOpeningBalance());
        entry.setReferenceNumber(entry.getEntryNumber());

        JournalEntry saved = journalEntryRepository.save(entry);

        // CUSTOMER = Receivable Debit, Equity Credit
        // VENDOR   = Equity Debit, Payable Credit
        JournalLine line1 = new JournalLine();
        JournalLine line2 = new JournalLine();

        if (party.getType() == PartyType.VENDOR) {
            line1.setAccount(openingEquity);
            line1.setDebit(party.getOpeningBalance());
            line1.setCredit(BigDecimal.ZERO);
            line1.setDescription("Opening Balance Equity");

            line2.setAccount(partyAccount);
            line2.setDebit(BigDecimal.ZERO);
            line2.setCredit(party.getOpeningBalance());
            line2.setDescription("Opening Balance - " + party.getName());
        } else {
            line1.setAccount(partyAccount);
            line1.setDebit(party.getOpeningBalance());
            line1.setCredit(BigDecimal.ZERO);
            line1.setDescription("Opening Balance - " + party.getName());

            line2.setAccount(openingEquity);
            line2.setDebit(BigDecimal.ZERO);
            line2.setCredit(party.getOpeningBalance());
            line2.setDescription("Opening Balance Equity");
        }

        line1.setJournalEntry(saved);
        line2.setJournalEntry(saved);

        journalLineRepository.save(line1);
        journalLineRepository.save(line2);

        if (party.getType() == PartyType.VENDOR) {
            updateBalance(openingEquity, line1);  // line1 = openingEquity
            updateBalance(partyAccount, line2);   // line2 = partyAccount
        } else {
            updateBalance(partyAccount, line1);   // line1 = partyAccount
            updateBalance(openingEquity, line2);  // line2 = openingEquity
        }

        // Account Balance update
//        updateBalance(partyAccount, line1);
//        updateBalance(openingEquity, line2);
    }

    private void updateBalance(Account account, JournalLine line) {
        switch (account.getType()) {
            case ASSET:
            case EXPENSE:
                account.setCurrentBalance(
                        account.getCurrentBalance()
                                .add(line.getDebit())
                                .subtract(line.getCredit())
                );
                break;
            case LIABILITY:
            case EQUITY:
            case REVENUE:
                account.setCurrentBalance(
                        account.getCurrentBalance()
                                .add(line.getCredit())
                                .subtract(line.getDebit())
                );
                break;
        }
        accountRepository.save(account);
    }

    private String generateCode() {
        return partyRepository.findAll().stream()
                .map(Party::getCode)
                .max(String::compareTo)
                .map(last -> {
                    int next = Integer.parseInt(last.replace("P-", "")) + 1;
                    return String.format("P-%04d", next);
                })
                .orElse("P-0001");
    }

    // Generate Entry Number
    private String generateEntryNumber() {
        return journalEntryRepository.findTopByOrderByIdDesc()
                .map(last -> {
                    String lastNumber = last.getEntryNumber().replace("JE-", "");
                    int next = Integer.parseInt(lastNumber) + 1;
                    return String.format("JE-%04d", next);
                })
                .orElse("JE-0001");
    }

    private void mapRequestToParty(PartyRequestDto request, Party party) {
        party.setName(request.getName());
        party.setType(request.getType());
        party.setNotes(request.getNotes());
        party.setCompanyName(request.getCompanyName());
        party.setContactPerson(request.getContactPerson());
        party.setJobPosition(request.getJobPosition());
        party.setEmail(request.getEmail());
        party.setPhone(request.getPhone());
        party.setMobile(request.getMobile());
        party.setStreet(request.getStreet());
        party.setCity(request.getCity());
        party.setState(request.getState());
        party.setCountry(request.getCountry() != null ? request.getCountry() : "Bangladesh");
        party.setCreditLimit(request.getCreditLimit() != null ? request.getCreditLimit() : BigDecimal.ZERO);
        party.setPaymentTerms(request.getPaymentTerms() != null ? request.getPaymentTerms() : 30);
        party.setOpeningBalance(request.getOpeningBalance() != null ? request.getOpeningBalance() : BigDecimal.ZERO);
        party.setCurrency(request.getCurrency() != null ? request.getCurrency() : "BDT");
        party.setBankAccountNo(request.getBankAccountNo());
        party.setBankName(request.getBankName());
        party.setBin(request.getBin());
        party.setTin(request.getTin());
        party.setVatRegistered(request.getVatRegistered() != null ? request.getVatRegistered() : false);
        party.setTradeLicenseNo(request.getTradeLicenseNo());
        party.setTradeLicenseExpiry(request.getTradeLicenseExpiry());
        party.setBinCertificateNo(request.getBinCertificateNo());
        party.setTinCertificateNo(request.getTinCertificateNo());
        party.setNidNo(request.getNidNo());
    }

                                             // --Mapper--


    private PartyResponseDto toResponse(Party party) {
        return PartyResponseDto.builder()
                .id(party.getId())
                .code(party.getCode())
                .name(party.getName())
                .type(party.getType())
                .isActive(party.getIsActive())
                .notes(party.getNotes())
                .companyName(party.getCompanyName())
                .contactPerson(party.getContactPerson())
                .jobPosition(party.getJobPosition())
                .email(party.getEmail())
                .phone(party.getPhone())
                .mobile(party.getMobile())
                .street(party.getStreet())
                .city(party.getCity())
                .state(party.getState())
                .country(party.getCountry())
                .creditLimit(party.getCreditLimit())
                .paymentTerms(party.getPaymentTerms())
                .openingBalance(party.getOpeningBalance())
                .currency(party.getCurrency())
                .bankAccountNo(party.getBankAccountNo())
                .bankName(party.getBankName())
                .bin(party.getBin())
                .tin(party.getTin())
                .vatRegistered(party.getVatRegistered())
                .tradeLicenseNo(party.getTradeLicenseNo())
                .tradeLicenseExpiry(party.getTradeLicenseExpiry())
                .binCertificateNo(party.getBinCertificateNo())
                .tinCertificateNo(party.getTinCertificateNo())
                .nidNo(party.getNidNo())
                .tradeLicenseUrl(party.getTradeLicenseUrl())
                .binCertificateUrl(party.getBinCertificateUrl())
                .tinCertificateUrl(party.getTinCertificateUrl())
                .nidUrl(party.getNidUrl())
                .build();
    }

}
