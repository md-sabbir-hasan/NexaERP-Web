package com.nexaerp.fixedasset.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepreciationEntryResponseDto {
    private Long id;
    private Long fixedAssetId;
    private String assetCode;
    private String assetName;
    private LocalDate periodDate;
    private BigDecimal depreciationAmount;
    private BigDecimal accumulatedDepreciationAfter;
    private BigDecimal bookValueAfter;
    private Long journalEntryId;
}