package com.nexaerp.currency.controller;

import com.nexaerp.common.response.ApiResponse;
import com.nexaerp.currency.dto.CurrencyConversionDto;
import com.nexaerp.currency.dto.ExchangeRateRequestDto;
import com.nexaerp.currency.dto.ExchangeRateResponseDto;
import com.nexaerp.currency.service.ExchangeRateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/exchange-rates")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @PostMapping
    public ResponseEntity<ApiResponse<ExchangeRateResponseDto>> create(
            @Valid @RequestBody ExchangeRateRequestDto request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Exchange rate created successfully",
                        exchangeRateService.create(request)
                )
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ExchangeRateResponseDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody ExchangeRateRequestDto request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Exchange rate updated successfully",
                        exchangeRateService.update(id, request)
                )
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ExchangeRateResponseDto>>> getAll() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        exchangeRateService.getAll()
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExchangeRateResponseDto>> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        exchangeRateService.getById(id)
                )
        );
    }

    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<ExchangeRateResponseDto>> getLatest(
            @RequestParam String from,
            @RequestParam String to
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        exchangeRateService.getLatestRate(from, to)
                )
        );
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<ExchangeRateResponseDto>>> getHistory(
            @RequestParam String from,
            @RequestParam String to
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        exchangeRateService.getHistory(from, to)
                )
        );
    }

    @GetMapping("/convert")
    public ResponseEntity<ApiResponse<CurrencyConversionDto>> convert(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        exchangeRateService.convert(
                                from,
                                to,
                                amount,
                                date
                        )
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id
    ) {
        exchangeRateService.delete(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Exchange rate deleted successfully", null
                )
        );
    }
}