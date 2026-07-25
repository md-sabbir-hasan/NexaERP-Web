package com.nexaerp.debitnote.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DebitNoteItemResponseDto {

    private Long id;

    private Long vendorBillItemId;

    private Long expenseAccountId;

    private String expenseAccountName;

    private String description;

    private BigDecimal quantity;

    private BigDecimal unitPrice;

    private BigDecimal discountPercent;

    private BigDecimal discountAmount;

    private BigDecimal vatRate;

    private BigDecimal vatAmount;

    private BigDecimal tdsRate;

    private BigDecimal tdsAmount;

    private BigDecimal subTotal;

    private BigDecimal lineTotal;

    private BigDecimal netAdjustment;
}