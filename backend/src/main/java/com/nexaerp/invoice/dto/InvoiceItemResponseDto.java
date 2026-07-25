package com.nexaerp.invoice.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItemResponseDto {
    private Long id;
    private Long productId;
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal discountPercent;
    private BigDecimal discountAmount;
    private BigDecimal vatRate;
    private BigDecimal vatAmount;
    private BigDecimal subTotal;
    private BigDecimal lineTotal;
}
