package com.nexaerp.account;

import com.nexaerp.account.dto.AccountRequestDto;
import com.nexaerp.account.dto.AccountResponseDto;
import com.nexaerp.audit.AuditAction;
import com.nexaerp.audit.AuditLogService;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.journal.JournalLineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService{

    private final AccountRepository accountRepository;
    private final AuditLogService auditLogService;
    private final JournalLineRepository journalLineRepository;

    @Override
    public AccountResponseDto create(AccountRequestDto request) {
        // Code unique check
        if (accountRepository.existsByCode(request.getCode())) {
            throw new BusinessRuleException("Account code already exists: " + request.getCode());
        }

        Account account = new Account();
        account.setCode(request.getCode());
        account.setName(request.getName());
        account.setDescription(request.getDescription());
        account.setType(request.getType());
        account.setIsActive(true);
        account.setIsDefault(false);

        // Parent set
        if (request.getParentId() != null) {
            Account parent = accountRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent account not found"));

            if (!parent.getIsActive()) {
                throw new BusinessRuleException("Cannot create child under inactive parent account");
            }

            if (!parent.getType().equals(request.getType())) {
                throw new BusinessRuleException("Child account type must match parent account type");
            }

            account.setParent(parent);
        }

        Account saved = accountRepository.save(account);
        auditLogService.log(
                AuditAction.CREATED,
                "ACCOUNT",
                saved.getId(),
                null,
                saved.getCode() + " - " + saved.getName()
        );

        return toResponse(saved);
    }

    @Override
    @Transactional
    public AccountResponseDto update(Long id, AccountRequestDto request) {

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        // Root account protection
        validateAccountModification(account);

        // Default account protection
        if (account.getIsDefault()) {
            throw new BusinessRuleException("Default system account cannot be updated");
        }

        // Code immutable
        if (!account.getCode().equals(request.getCode())) {
            throw new BusinessRuleException("Account code cannot be changed after creation");
        }

        // Type immutable
        if (!account.getType().equals(request.getType())) {
            throw new BusinessRuleException("Account type cannot be changed after creation");
        }

        account.setName(request.getName());
        account.setDescription(request.getDescription());

        Account saved = accountRepository.save(account);

        auditLogService.log(
                AuditAction.UPDATED,
                "ACCOUNT",
                saved.getId(),
                null,
                saved.getCode() + " - " + saved.getName()
        );

        return toResponse(saved);
    }

    @Override
    public AccountResponseDto getById(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        return toResponse(account);
    }

    @Override
    public List<AccountResponseDto> getAll() {
        return accountRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AccountResponseDto> getTree() {
        // only take root accounts, children comes auto recursively
        return accountRepository.findByParentIsNull()
                .stream()
                .map(this::toTreeResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponseDto> getByType(AccountType type) {

        return accountRepository.findByType(type)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        validateAccountModification(account);

        if (account.getIsDefault()) {
            throw new BusinessRuleException("Cannot deactivate a default system account");
        }

        if (!account.getIsActive()) {
            throw new BusinessRuleException("Account is already inactive");
        }

        if (accountRepository.existsByParentId(id)) {
            throw new BusinessRuleException("Cannot deactivate account with child accounts");
        }

        if (journalLineRepository.existsByAccountId(id)) {
            throw new BusinessRuleException("Cannot deactivate account used in journal entries");
        }

        account.setIsActive(false);
        accountRepository.save(account);

        auditLogService.log(
                AuditAction.DEACTIVATED,
                "ACCOUNT",
                account.getId(),
                "ACTIVE",
                "INACTIVE"
        );
    }

    @Override
    @Transactional
    public void activate(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (account.getIsActive()) {
            throw new BusinessRuleException("Account is already active");
        }

        if (account.getParent() != null && !account.getParent().getIsActive()) {
            throw new BusinessRuleException("Cannot activate account while parent account is inactive");
        }

        account.setIsActive(true);
        accountRepository.save(account);
        auditLogService.log(
                AuditAction.ACTIVATED,
                "ACCOUNT",
                account.getId(),
                "INACTIVE",
                "ACTIVE"
        );
    }


    @Override
    public List<AccountResponseDto> search(String keyword, AccountType type, Boolean active) {
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();

        List<Account> accounts;

        if (hasKeyword && type != null && active != null) {
            String key = keyword.trim();
            accounts = accountRepository
                    .findByTypeAndIsActiveAndNameContainingIgnoreCaseOrTypeAndIsActiveAndCodeContainingIgnoreCase(
                            type, active, key,
                            type, active, key
                    );
        } else if (hasKeyword && type != null) {
            String key = keyword.trim();
            accounts = accountRepository
                    .findByTypeAndNameContainingIgnoreCaseOrTypeAndCodeContainingIgnoreCase(
                            type, key,
                            type, key
                    );
        } else if (hasKeyword && active != null) {
            String key = keyword.trim();
            accounts = accountRepository
                    .findByIsActiveAndNameContainingIgnoreCaseOrIsActiveAndCodeContainingIgnoreCase(
                            active, key,
                            active, key
                    );
        } else if (type != null && active != null) {
            accounts = accountRepository.findByTypeAndIsActive(type, active);
        } else if (hasKeyword) {
            String key = keyword.trim();
            accounts = accountRepository
                    .findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(key, key);
        } else if (type != null) {
            accounts = accountRepository.findByType(type);
        } else if (active != null) {
            accounts = accountRepository.findByIsActive(active);
        } else {
            accounts = accountRepository.findAll();
        }

        return accounts.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }




    // =========================helper----------


    private boolean isRootAccount(Account account) {
        return account.getParent() == null;
    }

    private void validateAccountModification(Account account) {

        if (account.getParent() == null) {
            throw new BusinessRuleException(
                    "Root account cannot be modified");
        }

        if (account.getIsDefault()) {
            throw new BusinessRuleException(
                    "Default system account cannot be modified");
        }
    }

                                  // -- Mapper --

    private AccountResponseDto toResponse(Account account) {
        AccountResponseDto dto = new AccountResponseDto();
        dto.setId(account.getId());
        dto.setCode(account.getCode());
        dto.setName(account.getName());
        dto.setDescription(account.getDescription());
        dto.setType(account.getType());
        dto.setIsActive(account.getIsActive());
        dto.setIsDefault(account.getIsDefault());
        dto.setCurrentBalance(account.getCurrentBalance());
        dto.setHasChildren(account.getChildren() != null && !account.getChildren().isEmpty());


        if (account.getParent() != null) {
            dto.setParentId(account.getParent().getId());
            dto.setParentName(account.getParent().getName());
        }

        return dto;
    }

    private AccountResponseDto toTreeResponse(Account account) {
        AccountResponseDto dto = toResponse(account);

        if (account.getChildren() != null && !account.getChildren().isEmpty()) {
            dto.setChildren(
                    account.getChildren()
                            .stream()
                            .map(this::toTreeResponse) // Recursive
                            .collect(Collectors.toList())
            );
        }

        return dto;
    }
}
