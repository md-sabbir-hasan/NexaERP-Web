package com.nexaerp.banking.serviceimpl;

import com.nexaerp.account.Account;
import com.nexaerp.account.AccountRepository;
import com.nexaerp.banking.dto.BankAccountRequestDto;
import com.nexaerp.banking.dto.BankAccountResponseDto;
import com.nexaerp.banking.entity.BankAccount;
import com.nexaerp.banking.enums.BankAccountType;
import com.nexaerp.banking.repository.BankAccountRepository;
import com.nexaerp.banking.services.BankAccountService;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.journal.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BankAccountServiceImpl implements BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalLineRepository journalLineRepository;

    @Override
    @Transactional
    public BankAccountResponseDto create(BankAccountRequestDto request) {
        BankAccount account = BankAccount.builder()
                .accountName(request.getAccountName())
                .accountNumber(request.getAccountNumber())
                .bankName(request.getBankName())
                .branchName(request.getBranchName())
                .accountType(request.getAccountType())
                .currency(request.getCurrency() != null ? request.getCurrency() : "BDT")
                .openingBalance(request.getOpeningBalance() != null ?
                        request.getOpeningBalance() : BigDecimal.ZERO)
                .currentBalance(request.getOpeningBalance() != null ?
                        request.getOpeningBalance() : BigDecimal.ZERO)
                .isActive(true)
                .notes(request.getNotes())
                .mobileNumber(request.getMobileNumber())
                .walletProvider(request.getWalletProvider())
                .coaAccountId(request.getCoaAccountId())
                .build();

        BankAccount saved = bankAccountRepository.save(account);

        // Opening Balance Journal Entry
        if (request.getOpeningBalance() != null &&
                request.getOpeningBalance().compareTo(BigDecimal.ZERO) > 0 &&
                request.getCoaAccountId() != null) {
            createOpeningBalanceEntry(saved);
        }

        return toResponse(saved);
    }

    @Override
    @Transactional
    public BankAccountResponseDto update(Long id, BankAccountRequestDto request) {
        BankAccount account = bankAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bank account not found"));

        account.setAccountName(request.getAccountName());
        account.setAccountNumber(request.getAccountNumber());
        account.setBankName(request.getBankName());
        account.setBranchName(request.getBranchName());
        account.setNotes(request.getNotes());
        account.setMobileNumber(request.getMobileNumber());
        account.setWalletProvider(request.getWalletProvider());

        return toResponse(bankAccountRepository.save(account));
    }

    @Override
    public BankAccountResponseDto getById(Long id) {
        BankAccount account = bankAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bank account not found"));
        return toResponse(account);
    }

    @Override
    public List<BankAccountResponseDto> getAll() {
        return bankAccountRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<BankAccountResponseDto> getByType(BankAccountType type) {
        return bankAccountRepository.findByAccountType(type)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deactivate(Long id) {

        BankAccount account = bankAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bank account not found"));

        if (!Boolean.TRUE.equals(account.getIsActive())) {
            throw new BusinessRuleException("Bank account is already inactive");
        }

        account.setIsActive(false);
        bankAccountRepository.save(account);

    }

    @Override
    public void activate(Long id) {

        BankAccount account = bankAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bank account not found"));

        if (Boolean.TRUE.equals(account.getIsActive())) {
            throw new BusinessRuleException("Bank account is already active");
        }

        account.setIsActive(true);
        bankAccountRepository.save(account);

    }




    // ====Private Helper==========


    private void createOpeningBalanceEntry(BankAccount bankAccount) {

        Account coaAccount = accountRepository.findById(bankAccount.getCoaAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("COA account not found"));

        Account openingEquity = accountRepository.findByCode("3100")
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Opening Balance Equity account not found"));

        JournalEntry entry = new JournalEntry();
        entry.setEntryNumber(generateJournalNumber());
        entry.setDate(LocalDate.now());
        entry.setDescription("Opening Balance - " + bankAccount.getAccountName());
        entry.setType(JournalEntryType.GENERAL);
        entry.setStatus(JournalStatus.POSTED);
        entry.setSourceType(JournalSourceType.MANUAL);
        entry.setReferenceNumber(entry.getEntryNumber());
        entry.setTotalAmount(bankAccount.getOpeningBalance());

        JournalEntry saved = journalEntryRepository.save(entry);

        // Dr COA Account (Bank/Cash)
        JournalLine line1 = new JournalLine();
        line1.setJournalEntry(saved);
        line1.setAccount(coaAccount);
        line1.setDebit(bankAccount.getOpeningBalance());
        line1.setCredit(BigDecimal.ZERO);
        line1.setDescription("Opening Balance - " + bankAccount.getAccountName());
        journalLineRepository.save(line1);

        // Cr Opening Balance Equity
        JournalLine line2 = new JournalLine();
        line2.setJournalEntry(saved);
        line2.setAccount(openingEquity);
        line2.setDebit(BigDecimal.ZERO);
        line2.setCredit(bankAccount.getOpeningBalance());
        line2.setDescription("Opening Balance Equity");
        journalLineRepository.save(line2);

        // Update COA account balance
        coaAccount.setCurrentBalance(
                coaAccount.getCurrentBalance().add(bankAccount.getOpeningBalance()));
        accountRepository.save(coaAccount);

        // Update Opening Equity balance
        openingEquity.setCurrentBalance(
                openingEquity.getCurrentBalance().add(bankAccount.getOpeningBalance()));
        accountRepository.save(openingEquity);
    }

    private String generateJournalNumber() {
        return journalEntryRepository.findTopByOrderByIdDesc()
                .map(last -> {
                    String lastNumber = last.getEntryNumber().replace("JE-", "");
                    int next = Integer.parseInt(lastNumber) + 1;
                    return String.format("JE-%04d", next);
                })
                .orElse("JE-0001");
    }

    private BankAccountResponseDto toResponse(BankAccount account) {
        return BankAccountResponseDto.builder()
                .id(account.getId())
                .accountName(account.getAccountName())
                .accountNumber(account.getAccountNumber())
                .bankName(account.getBankName())
                .branchName(account.getBranchName())
                .accountType(account.getAccountType())
                .currency(account.getCurrency())
                .openingBalance(account.getOpeningBalance())
                .currentBalance(account.getCurrentBalance())
                .isActive(account.getIsActive())
                .notes(account.getNotes())
                .mobileNumber(account.getMobileNumber())
                .walletProvider(account.getWalletProvider())
                .coaAccountId(account.getCoaAccountId())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }

}
