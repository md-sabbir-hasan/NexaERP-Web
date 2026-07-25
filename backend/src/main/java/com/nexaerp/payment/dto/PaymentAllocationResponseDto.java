package com.nexaerp.payment.dto;

import com.nexaerp.payment.PaymentReferenceType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentAllocationResponseDto {
    private Long id;
    private PaymentReferenceType referenceType;
    private Long referenceId;
    private BigDecimal allocatedAmount;
    private LocalDateTime createdAt;
}
