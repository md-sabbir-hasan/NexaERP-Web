package com.nexaerp.account;


import com.nexaerp.account.dto.AccountRequestDto;
import com.nexaerp.account.dto.AccountResponseDto;
import com.nexaerp.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor

public class AccountController {

    private final AccountService  accountService;

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_ACCOUNT')")
    public ResponseEntity<ApiResponse<AccountResponseDto>> create(
            @Valid @RequestBody AccountRequestDto request) {
        AccountResponseDto response = accountService.create(request);
        return ResponseEntity.ok(ApiResponse.success("Account created", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VIEW_ACCOUNTS')")
    public ResponseEntity<ApiResponse<AccountResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(accountService.getById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_ACCOUNTS')")
    public ResponseEntity<ApiResponse<List<AccountResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(accountService.getAll()));
    }

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('VIEW_ACCOUNTS')")
    public ResponseEntity<ApiResponse<List<AccountResponseDto>>> getTree() {
        return ResponseEntity.ok(ApiResponse.success(accountService.getTree()));
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("hasAnyAuthority('VIEW_ACCOUNTS', 'LOOKUP_ACCOUNTS')")
    public ResponseEntity<ApiResponse<List<AccountResponseDto>>> getByType(
            @PathVariable AccountType type) {
        return ResponseEntity.ok(ApiResponse.success(accountService.getByType(type)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EDIT_ACCOUNT')")
    public ResponseEntity<ApiResponse<AccountResponseDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody AccountRequestDto request
    ) {
        AccountResponseDto response = accountService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Account updated", response));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('DEACTIVATE_ACCOUNT')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        accountService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success("Account deactivated", null));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('VIEW_ACCOUNTS', 'LOOKUP_ACCOUNTS')")
    public ResponseEntity<ApiResponse<List<AccountResponseDto>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) AccountType type,
            @RequestParam(required = false) Boolean active
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                accountService.search(keyword, type, active)
        ));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('EDIT_ACCOUNT')")
    public ResponseEntity<ApiResponse<Void>> activate(@PathVariable Long id) {
        accountService.activate(id);
        return ResponseEntity.ok(ApiResponse.success("Account activated", null));
    }

}
