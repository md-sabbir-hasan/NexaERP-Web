package com.nexaerp.vendorbill;


import com.nexaerp.common.response.ApiResponse;
import com.nexaerp.vendorbill.dto.VendorBillRequestDto;
import com.nexaerp.vendorbill.dto.VendorBillResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendor-bills")
@RequiredArgsConstructor
public class VendorBillController {
    private final VendorBillService vendorBillService;

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_VENDOR_BILL')")
    public ResponseEntity<ApiResponse<VendorBillResponseDto>> create(
            @Valid @RequestBody VendorBillRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Vendor bill created",
                vendorBillService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EDIT_VENDOR_BILL')")
    public ResponseEntity<ApiResponse<VendorBillResponseDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody VendorBillRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Vendor bill updated",
                vendorBillService.update(id, request)));
    }


    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VIEW_VENDOR_BILL')")
    public ResponseEntity<ApiResponse<VendorBillResponseDto>> getById(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                vendorBillService.getById(id)));
    }


    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_VENDOR_BILL')")
    public ResponseEntity<ApiResponse<List<VendorBillResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(
                vendorBillService.getAll()));
    }

    @GetMapping("/party/{partyId}")
    @PreAuthorize("hasAuthority('VIEW_VENDOR_BILL')")
    public ResponseEntity<ApiResponse<List<VendorBillResponseDto>>> getByParty(
            @PathVariable Long partyId) {
        return ResponseEntity.ok(ApiResponse.success(
                vendorBillService.getByParty(partyId)));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('VIEW_VENDOR_BILL')")
    public ResponseEntity<ApiResponse<List<VendorBillResponseDto>>> getByStatus(
            @PathVariable VendorBillStatus status) {
        return ResponseEntity.ok(ApiResponse.success(
                vendorBillService.getByStatus(status)));
    }

    @GetMapping("/type/{billType}")
    @PreAuthorize("hasAuthority('VIEW_VENDOR_BILL')")
    public ResponseEntity<ApiResponse<List<VendorBillResponseDto>>> getByBillType(
            @PathVariable VendorBillType billType) {
        return ResponseEntity.ok(ApiResponse.success(
                vendorBillService.getByBillType(billType)));
    }


    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('APPROVE_VENDOR_BILL')")
    public ResponseEntity<ApiResponse<VendorBillResponseDto>> approve(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Vendor bill approved",
                vendorBillService.approve(id)));
    }


    @PostMapping("/{id}/post")
    @PreAuthorize("hasAuthority('POST_VENDOR_BILL')")
    public ResponseEntity<ApiResponse<VendorBillResponseDto>> post(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Vendor bill posted",
                vendorBillService.post(id)));
    }


    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('CANCEL_VENDOR_BILL')")
    public ResponseEntity<ApiResponse<VendorBillResponseDto>> cancel(
            @PathVariable Long id,
            @RequestParam VendorBillCancelledReason reason) {
        return ResponseEntity.ok(ApiResponse.success("Vendor bill cancelled",
                vendorBillService.cancel(id, reason)));
    }
}
