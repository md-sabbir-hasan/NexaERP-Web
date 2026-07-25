package com.nexaerp.currency.controller;

import com.nexaerp.common.response.ApiResponse;
import com.nexaerp.currency.dto.CurrencyRequestDto;
import com.nexaerp.currency.dto.CurrencyResponseDto;
import com.nexaerp.currency.service.CurrencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/currencies")
@RequiredArgsConstructor
public class CurrencyController {

    private final CurrencyService currencyService;

    @PostMapping
    public ResponseEntity<ApiResponse<CurrencyResponseDto>> create(
            @Valid @RequestBody CurrencyRequestDto request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Currency created successfully",
                        currencyService.create(request)
                )
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CurrencyResponseDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody CurrencyRequestDto request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Currency updated successfully",
                        currencyService.update(id, request)
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CurrencyResponseDto>> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        currencyService.getById(id)
                )
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CurrencyResponseDto>>> getAll() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        currencyService.getAll()
                )
        );
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<CurrencyResponseDto>>> getActive() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        currencyService.getActive()
                )
        );
    }

    @GetMapping("/base")
    public ResponseEntity<ApiResponse<CurrencyResponseDto>> getBaseCurrency() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        currencyService.getBaseCurrency()
                )
        );
    }

    @PatchMapping("/{id}/set-base")
    public ResponseEntity<ApiResponse<Void>> setBaseCurrency(
            @PathVariable Long id
    ) {
        currencyService.setBaseCurrency(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Base currency updated successfully",
                        null
                )
        );
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @PathVariable Long id
    ) {
        currencyService.deactivate(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Currency deactivated successfully",
                        null
                )
        );
    }
}