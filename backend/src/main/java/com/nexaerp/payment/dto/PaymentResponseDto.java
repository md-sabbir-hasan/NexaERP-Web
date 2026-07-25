package com.nexaerp.payment.dto;

import com.nexaerp.payment.PaymentMethod;
import com.nexaerp.payment.PaymentStatus;
import com.nexaerp.payment.PaymentType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponseDto {
    private Long id;
    private String paymentNumber;
    private LocalDate paymentDate;
    private PaymentType paymentType;

    private Long partyId;
    private String partyName;

    private Long accountId;
    private String accountName;

    private BigDecimal amount;
    private BigDecimal allocatedAmount;
    private BigDecimal unallocatedAmount;

    private String currencyCode;
    private BigDecimal exchangeRate;
    private PaymentMethod paymentMethod;
    private String transactionRef;
    private String notes;
    private PaymentStatus status;

    private LocalDateTime postedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<PaymentAllocationResponseDto> allocations;
}
