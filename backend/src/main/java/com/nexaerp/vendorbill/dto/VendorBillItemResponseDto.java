package com.nexaerp.vendorbill.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorBillItemResponseDto {
    private Long id;

    private Long productId;

    private Long expenseAccountId;
    private String expenseAccountName;
    private String expenseAccountCode;

    private Long costCenterId;

    // Item details
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitPrice;

    // Discount
    private BigDecimal discountPercent;
    private BigDecimal discountAmount;

    // VAT
    private BigDecimal vatRate;
    private BigDecimal vatAmount;

    // TDS
    private BigDecimal tdsRate;
    private BigDecimal tdsAmount;

    // Totals
    private BigDecimal subTotal;
    private BigDecimal lineTotal;
}
