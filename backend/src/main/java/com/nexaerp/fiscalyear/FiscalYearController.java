package com.nexaerp.fiscalyear;

import com.nexaerp.common.response.ApiResponse;
import com.nexaerp.fiscalyear.dto.FiscalYearRequestDto;
import com.nexaerp.fiscalyear.dto.FiscalYearResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/fiscal-years")
@RequiredArgsConstructor
public class FiscalYearController {

    private final FiscalYearService fiscalYearService;

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_FISCAL_YEAR')")
    public ResponseEntity<ApiResponse<FiscalYearResponseDto>> create(
            @Valid @RequestBody FiscalYearRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Fiscal year created",
                fiscalYearService.create(request)
        ));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EDIT_FISCAL_YEAR')")
    public ResponseEntity<ApiResponse<FiscalYearResponseDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody FiscalYearRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Fiscal year updated",
                fiscalYearService.update(id, request)
        ));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_FISCAL_YEAR')")
    public ResponseEntity<ApiResponse<List<FiscalYearResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(fiscalYearService.getAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VIEW_FISCAL_YEAR')")
    public ResponseEntity<ApiResponse<FiscalYearResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(fiscalYearService.getById(id)));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('VIEW_FISCAL_YEAR')")
    public ResponseEntity<ApiResponse<FiscalYearResponseDto>> getActive() {
        return ResponseEntity.ok(ApiResponse.success(fiscalYearService.getActive()));
    }

    @GetMapping("/for-date")
    @PreAuthorize("hasAuthority('VIEW_FISCAL_YEAR')")
    public ResponseEntity<ApiResponse<FiscalYearResponseDto>> getByDate(
            @RequestParam LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(fiscalYearService.getByDate(date)));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('ACTIVATE_FISCAL_YEAR')")
    public ResponseEntity<ApiResponse<FiscalYearResponseDto>> activate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Fiscal year activated",
                fiscalYearService.activate(id)
        ));
    }

    @PatchMapping("/{id}/close")
    @PreAuthorize("hasAuthority('CLOSE_FISCAL_YEAR')")
    public ResponseEntity<ApiResponse<FiscalYearResponseDto>> close(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Fiscal year closed",
                fiscalYearService.close(id)
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE_FISCAL_YEAR')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        fiscalYearService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Fiscal year deleted", null));
    }
}
