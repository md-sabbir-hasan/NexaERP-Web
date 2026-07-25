package com.nexaerp.account;

import com.nexaerp.account.dto.AccountRequestDto;
import com.nexaerp.account.dto.AccountResponseDto;

import java.util.List;

public interface AccountService {
    AccountResponseDto create(AccountRequestDto request);
    AccountResponseDto update(Long id, AccountRequestDto request);

    AccountResponseDto getById(Long id);

    List<AccountResponseDto> getAll();

    List<AccountResponseDto> getTree(); // Hierarchical

    List<AccountResponseDto> getByType(AccountType type);

    void deactivate(Long id);

    List<AccountResponseDto> search(String keyword, AccountType type, Boolean active);

    void activate(Long id);
}
