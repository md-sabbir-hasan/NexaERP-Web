package com.nexaerp.fixedasset.dto;

import com.nexaerp.fixedasset.AssetStatus;
import com.nexaerp.fixedasset.DepreciationMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FixedAssetResponseDto {
    private Long id;
    private String assetCode;
    private String name;
    private String description;

    private Long assetAccountId;
    private String assetAccountName;
    private Long depreciationExpenseAccountId;
    private String depreciationExpenseAccountName;
    private Long accumulatedDepreciationAccountId;
    private String accumulatedDepreciationAccountName;

    private LocalDate purchaseDate;
    private BigDecimal purchaseCost;
    private BigDecimal salvageValue;
    private Integer usefulLifeYears;
    private DepreciationMethod depreciationMethod;
    private BigDecimal reducingBalanceRate;

    private BigDecimal accumulatedDepreciation;
    private BigDecimal bookValue;

    private AssetStatus status;
    private LocalDate lastDepreciationDate;

    private LocalDate disposalDate;
    private BigDecimal disposalProceeds;
    private BigDecimal disposalGainLoss;

    private LocalDateTime createdAt;
}
