package com.nexaerp.vendorbill.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
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
public class VendorBillItemRequestDto {
    private Long productId;

    @NotNull(message = "Expense account is required")
    private Long expenseAccountId;

    private Long costCenterId;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.01", message = "Quantity must be greater than 0")
    private BigDecimal quantity;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.0")
    private BigDecimal unitPrice;

    private BigDecimal discountPercent = BigDecimal.ZERO;

    private BigDecimal vatRate = BigDecimal.ZERO;

    private BigDecimal tdsRate = BigDecimal.ZERO;
}
