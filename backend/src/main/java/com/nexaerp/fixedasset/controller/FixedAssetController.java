package com.nexaerp.fixedasset.controller;

import com.nexaerp.common.response.ApiResponse;
import com.nexaerp.fixedasset.dto.*;
import com.nexaerp.fixedasset.services.FixedAssetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/fixed-assets")
@RequiredArgsConstructor
public class FixedAssetController {

    private final FixedAssetService fixedAssetService;

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_FIXED_ASSET')")
    public ResponseEntity<ApiResponse<FixedAssetResponseDto>> create(
            @Valid @RequestBody FixedAssetRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Fixed asset registered", fixedAssetService.create(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VIEW_FIXED_ASSET')")
    public ResponseEntity<ApiResponse<FixedAssetResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(fixedAssetService.getById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_FIXED_ASSET')")
    public ResponseEntity<ApiResponse<List<FixedAssetResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(fixedAssetService.getAll()));
    }

    @GetMapping("/{id}/depreciation-history")
    @PreAuthorize("hasAuthority('VIEW_FIXED_ASSET')")
    public ResponseEntity<ApiResponse<List<DepreciationEntryResponseDto>>> getDepreciationHistory(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(fixedAssetService.getDepreciationHistory(id)));
    }

    @PostMapping("/{id}/run-depreciation")
    @PreAuthorize("hasAuthority('RUN_DEPRECIATION')")
    public ResponseEntity<ApiResponse<DepreciationEntryResponseDto>> runDepreciation(
            @PathVariable Long id, @Valid @RequestBody DepreciationRunRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Depreciation posted",
                fixedAssetService.runDepreciation(id, request.getAsOfDate())));
    }

    @PostMapping("/run-depreciation-all")
    @PreAuthorize("hasAuthority('RUN_DEPRECIATION')")
    public ResponseEntity<ApiResponse<List<DepreciationEntryResponseDto>>> runDepreciationForAll(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        return ResponseEntity.ok(ApiResponse.success("Depreciation run completed",
                fixedAssetService.runDepreciationForAll(asOfDate)));
    }

    @PostMapping("/{id}/dispose")
    @PreAuthorize("hasAuthority('DISPOSE_FIXED_ASSET')")
    public ResponseEntity<ApiResponse<FixedAssetResponseDto>> dispose(
            @PathVariable Long id, @Valid @RequestBody AssetDisposalRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Asset disposed", fixedAssetService.dispose(id, request)));
    }
}
