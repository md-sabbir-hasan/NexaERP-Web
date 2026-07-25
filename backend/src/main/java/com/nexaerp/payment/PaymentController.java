package com.nexaerp.payment;

import com.nexaerp.common.response.ApiResponse;
import com.nexaerp.payment.dto.PartyOutstandingSummaryDto;
import com.nexaerp.payment.dto.PaymentRequestDto;
import com.nexaerp.payment.dto.PaymentResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_PAYMENT')")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> create(
            @Valid @RequestBody PaymentRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Payment created",
                paymentService.create(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VIEW_PAYMENT')")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> getById(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.getById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_PAYMENT')")
    public ResponseEntity<ApiResponse<List<PaymentResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.getAll()));
    }

    @GetMapping("/party/{partyId}")
    @PreAuthorize("hasAuthority('VIEW_PAYMENT')")
    public ResponseEntity<ApiResponse<List<PaymentResponseDto>>> getByParty(
            @PathVariable Long partyId) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.getByParty(partyId)));
    }

    @GetMapping("/outstanding-summary")
    public ResponseEntity<PartyOutstandingSummaryDto> getOutstandingSummary(
            @RequestParam Long partyId,
            @RequestParam PaymentType paymentType
    ) {
        return ResponseEntity.ok(
                paymentService.getOutstandingSummary(partyId, paymentType)
        );
    }

    @PostMapping("/{id}/post")
    @PreAuthorize("hasAuthority('POST_PAYMENT')")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> post(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Payment posted",
                paymentService.post(id)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('CANCEL_PAYMENT')")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> cancel(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Payment cancelled",
                paymentService.cancel(id)));
    }
}
