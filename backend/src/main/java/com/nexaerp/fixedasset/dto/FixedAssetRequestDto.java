package com.nexaerp.fixedasset.dto;

import com.nexaerp.fixedasset.DepreciationMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FixedAssetRequestDto {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotNull(message = "Asset account is required")
    private Long assetAccountId;

    @NotNull(message = "Depreciation expense account is required")
    private Long depreciationExpenseAccountId;

    @NotNull(message = "Accumulated depreciation account is required")
    private Long accumulatedDepreciationAccountId;

    // The account the purchase money came from/goes to — Cash, Bank, or
    // Accounts Payable if bought on credit. Used to post the initial purchase entry.
    @NotNull(message = "Payment source account is required")
    private Long paymentSourceAccountId;

    @NotNull(message = "Purchase date is required")
    private LocalDate purchaseDate;

    @NotNull(message = "Purchase cost is required")
    @DecimalMin(value = "0.01", message = "Purchase cost must be greater than 0")
    private BigDecimal purchaseCost;

    @NotNull(message = "Salvage value is required")
    @DecimalMin(value = "0.0", message = "Salvage value cannot be negative")
    private BigDecimal salvageValue;

    @NotNull(message = "Useful life is required")
    @Min(value = 1, message = "Useful life must be at least 1 year")
    private Integer usefulLifeYears;

    @NotNull(message = "Depreciation method is required")
    private DepreciationMethod depreciationMethod;

    // required only when depreciationMethod = REDUCING_BALANCE
    private BigDecimal reducingBalanceRate;
}
