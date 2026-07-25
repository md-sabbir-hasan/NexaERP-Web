package com.nexaerp.payment.dto;

import com.nexaerp.payment.PaymentMethod;
import com.nexaerp.payment.PaymentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDto {
    @NotNull(message = "Party is required")
    private Long partyId;

    @NotNull(message = "Account is required")
    private Long accountId;

    @NotNull(message = "Payment date is required")
    private LocalDate paymentDate;

    @NotNull(message = "Payment type is required")
    private PaymentType paymentType;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    private String currencyCode;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private String transactionRef;
    private String notes;

    // If true, system will auto-allocate using FIFO (oldest due first)
    // and the "allocations" list below will be ignored
    private Boolean autoAllocate = false;

    // Required only when autoAllocate = false
    private List<PaymentAllocationRequestDto> allocations;
}
