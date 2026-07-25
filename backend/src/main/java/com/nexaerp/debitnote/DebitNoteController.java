package com.nexaerp.debitnote;

import com.nexaerp.common.response.ApiResponse;
import com.nexaerp.debitnote.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/debit-notes")
@RequiredArgsConstructor
public class DebitNoteController {
    private final DebitNoteService service;

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_DEBIT_NOTE')")
    public ResponseEntity<ApiResponse<DebitNoteResponseDto>> create(@Valid @RequestBody DebitNoteRequestDto r) {
        return ResponseEntity.ok(ApiResponse.success("Debit note created", service.create(r)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EDIT_DEBIT_NOTE')")
    public ResponseEntity<ApiResponse<DebitNoteResponseDto>> update(@PathVariable Long id, @Valid @RequestBody DebitNoteRequestDto r) {
        return ResponseEntity.ok(ApiResponse.success("Debit note updated", service.update(id, r)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VIEW_DEBIT_NOTE')")
    public ResponseEntity<ApiResponse<DebitNoteResponseDto>> one(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.getById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_DEBIT_NOTE')")
    public ResponseEntity<ApiResponse<List<DebitNoteResponseDto>>> all() {
        return ResponseEntity.ok(ApiResponse.success(service.getAll()));
    }

    @GetMapping("/vendorBill/{vendorBillId}")
    @PreAuthorize("hasAuthority('VIEW_DEBIT_NOTE')")
    public ResponseEntity<ApiResponse<List<DebitNoteResponseDto>>> byVendorBill(@PathVariable Long vendorBillId) {
        return ResponseEntity.ok(ApiResponse.success(service.getByVendorBill(vendorBillId)));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('APPROVE_DEBIT_NOTE')")
    public ResponseEntity<ApiResponse<DebitNoteResponseDto>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Debit note approved", service.approve(id)));
    }

    @PatchMapping("/{id}/post")
    @PreAuthorize("hasAuthority('POST_DEBIT_NOTE')")
    public ResponseEntity<ApiResponse<DebitNoteResponseDto>> post(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Debit note posted", service.post(id)));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('CANCEL_DEBIT_NOTE')")
    public ResponseEntity<ApiResponse<DebitNoteResponseDto>> cancel(@PathVariable Long id, @RequestParam DebitNoteCancelledReason reason) {
        return ResponseEntity.ok(ApiResponse.success("Debit note cancelled", service.cancel(id, reason)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE_DEBIT_NOTE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Debit note deleted", null));
    }
}
