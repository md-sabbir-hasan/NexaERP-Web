package com.nexaerp.invoice;

import com.nexaerp.common.response.ApiResponse;
import com.nexaerp.approval.ApprovalService;
import com.nexaerp.approval.dto.ApprovalRequestResponseDto;
import com.nexaerp.fileupload.dto.FileUploadResponseDto;
import com.nexaerp.invoice.dto.InvoiceRequestDto;
import com.nexaerp.invoice.dto.InvoiceResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {
    private final InvoiceService invoiceService;
    private final ApprovalService approvalService;

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_INVOICE')")
    public ResponseEntity<ApiResponse<InvoiceResponseDto>> create(
            @Valid @RequestBody InvoiceRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Invoice created",
                invoiceService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EDIT_INVOICE')")
    public ResponseEntity<ApiResponse<InvoiceResponseDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody InvoiceRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Invoice updated",
                invoiceService.update(id, request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VIEW_INVOICE')")
    public ResponseEntity<ApiResponse<InvoiceResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_INVOICE')")
    public ResponseEntity<ApiResponse<List<InvoiceResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getAll()));
    }

    @GetMapping("/party/{partyId}")
    @PreAuthorize("hasAuthority('VIEW_INVOICE')")
    public ResponseEntity<ApiResponse<List<InvoiceResponseDto>>> getByParty(
            @PathVariable Long partyId) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getByParty(partyId)));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('VIEW_INVOICE')")
    public ResponseEntity<ApiResponse<List<InvoiceResponseDto>>> getByStatus(
            @PathVariable InvoiceStatus status) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getByStatus(status)));
    }

    @PostMapping("/{id}/post")
    @PreAuthorize("hasAuthority('POST_INVOICE')")
    public ResponseEntity<ApiResponse<InvoiceResponseDto>> post(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Invoice posted",
                invoiceService.post(id)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('CANCEL_INVOICE')")
    public ResponseEntity<ApiResponse<InvoiceResponseDto>> cancel(
            @PathVariable Long id,
            @RequestParam CancelledReason reason) {
        return ResponseEntity.ok(ApiResponse.success("Invoice cancelled",
                invoiceService.cancel(id, reason)));
    }


    @PostMapping("/{id}/attachment")
    @PreAuthorize("hasAuthority('EDIT_INVOICE')")
    public ResponseEntity<ApiResponse<FileUploadResponseDto>> uploadAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {

        FileUploadResponseDto response = invoiceService.uploadAttachment(id, file);

        return ResponseEntity.ok(ApiResponse.success(
                "Attachment uploaded", response));
    }

    @PostMapping("/{id}/submit-approval")
    @PreAuthorize("hasAnyAuthority('CREATE_INVOICE','EDIT_INVOICE')")
    public ResponseEntity<ApiResponse<ApprovalRequestResponseDto>> submitApproval(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Invoice submitted for approval",
                approvalService.submitInvoice(id)));
    }
}
