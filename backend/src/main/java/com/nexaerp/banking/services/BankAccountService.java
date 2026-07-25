package com.nexaerp.banking.services;

import com.nexaerp.banking.dto.BankAccountRequestDto;
import com.nexaerp.banking.dto.BankAccountResponseDto;
import com.nexaerp.banking.enums.BankAccountType;

import java.util.List;

public interface BankAccountService {
    BankAccountResponseDto create(BankAccountRequestDto request);
    BankAccountResponseDto update(Long id, BankAccountRequestDto request);
    BankAccountResponseDto getById(Long id);
    List<BankAccountResponseDto> getAll();
    List<BankAccountResponseDto> getByType(BankAccountType type);
    void deactivate(Long id);
    void activate(Long id);
}
