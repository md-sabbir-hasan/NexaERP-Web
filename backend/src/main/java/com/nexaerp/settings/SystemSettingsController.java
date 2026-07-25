package com.nexaerp.settings;


import com.nexaerp.account.Account;
import com.nexaerp.account.AccountRepository;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.common.response.ApiResponse;
import com.nexaerp.settings.dto.SettingResponseDto;
import com.nexaerp.settings.dto.SettingUpdateRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SystemSettingsController {
    private final SystemSettingRepository systemSettingRepository;
    private final SystemSettingsService systemSettingsService;
    private final AccountRepository accountRepository;

    // Get all settings
    @PreAuthorize("hasAuthority('MANAGE_SETTINGS')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<SettingResponseDto>>> getAll() {
        List<SettingResponseDto> settings = systemSettingRepository.findAll()
                .stream()
                .map(s -> SettingResponseDto.builder()
                        .id(s.getId())
                        .key(s.getKey())
                        .value(s.getValue())
                        .description(s.getDescription())
                        .updatedAt(s.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    // Update a setting
    @PreAuthorize("hasAuthority('MANAGE_SETTINGS')")
    @PutMapping("/{key}")
    public ResponseEntity<ApiResponse<SettingResponseDto>> update(
            @PathVariable SettingKey key,
            @Valid @RequestBody SettingUpdateRequestDto request) {

        // Only the "default account" group of keys reference an Account row.
        // Everything else (company name, currency, feature flags, ...) is a
        // plain value and doesn't need account validation.
        if (key.isAccountReference()) {
            Long accountId;
            try {
                accountId = Long.parseLong(request.getValue());
            } catch (NumberFormatException e) {
                throw new BusinessRuleException(
                        "Value for " + key + " must be a valid account ID");
            }

            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Account not found: " + accountId));

            if (!account.getIsActive()) {
                throw new BusinessRuleException("Account is not active");
            }
        }

        systemSettingsService.updateSetting(key, request.getValue());

        SystemSetting updated = systemSettingRepository.findByKey(key).get();

        return ResponseEntity.ok(ApiResponse.success("Setting updated",
                SettingResponseDto.builder()
                        .id(updated.getId())
                        .key(updated.getKey())
                        .value(updated.getValue())
                        .description(updated.getDescription())
                        .updatedAt(updated.getUpdatedAt())
                        .build()));
    }

}