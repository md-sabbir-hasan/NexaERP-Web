package com.nexaerp.creditnote;

import com.nexaerp.common.response.ApiResponse;
import com.nexaerp.creditnote.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/credit-notes")
@RequiredArgsConstructor
public class CreditNoteController {
    private final CreditNoteService service;

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_CREDIT_NOTE')")
    public ResponseEntity<ApiResponse<CreditNoteResponseDto>> create(@Valid @RequestBody CreditNoteRequestDto r) {
        return ResponseEntity.ok(ApiResponse.success("Credit note created", service.create(r)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EDIT_CREDIT_NOTE')")
    public ResponseEntity<ApiResponse<CreditNoteResponseDto>> update(@PathVariable Long id, @Valid @RequestBody CreditNoteRequestDto r) {
        return ResponseEntity.ok(ApiResponse.success("Credit note updated", service.update(id, r)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VIEW_CREDIT_NOTE')")
    public ResponseEntity<ApiResponse<CreditNoteResponseDto>> one(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.getById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_CREDIT_NOTE')")
    public ResponseEntity<ApiResponse<List<CreditNoteResponseDto>>> all() {
        return ResponseEntity.ok(ApiResponse.success(service.getAll()));
    }

    @GetMapping("/invoice/{invoiceId}")
    @PreAuthorize("hasAuthority('VIEW_CREDIT_NOTE')")
    public ResponseEntity<ApiResponse<List<CreditNoteResponseDto>>> byInvoice(@PathVariable Long invoiceId) {
        return ResponseEntity.ok(ApiResponse.success(service.getByInvoice(invoiceId)));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('APPROVE_CREDIT_NOTE')")
    public ResponseEntity<ApiResponse<CreditNoteResponseDto>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Credit note approved", service.approve(id)));
    }

    @PatchMapping("/{id}/post")
    @PreAuthorize("hasAuthority('POST_CREDIT_NOTE')")
    public ResponseEntity<ApiResponse<CreditNoteResponseDto>> post(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Credit note posted", service.post(id)));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('CANCEL_CREDIT_NOTE')")
    public ResponseEntity<ApiResponse<CreditNoteResponseDto>> cancel(@PathVariable Long id, @RequestParam CreditNoteCancelledReason reason) {
        return ResponseEntity.ok(ApiResponse.success("Credit note cancelled", service.cancel(id, reason)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE_CREDIT_NOTE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Credit note deleted", null));
    }
}
