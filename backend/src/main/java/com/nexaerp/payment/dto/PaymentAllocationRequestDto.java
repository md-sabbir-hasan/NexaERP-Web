package com.nexaerp.payment.dto;

import com.nexaerp.payment.PaymentReferenceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAllocationRequestDto {
    @NotNull(message = "Reference type is required")
    private PaymentReferenceType referenceType;

    @NotNull(message = "Reference id is required")
    private Long referenceId;

    @NotNull(message = "Allocated amount is required")
    @DecimalMin(value = "0.01", message = "Allocated amount must be greater than 0")
    private BigDecimal allocatedAmount;
}
