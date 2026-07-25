package com.nexaerp.fixedasset.dto;

import jakarta.validation.constraints.DecimalMin;
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
public class AssetDisposalRequestDto {
    @NotNull(message = "Disposal date is required")
    private LocalDate disposalDate;

    // Cash/bank/receivable received for the asset (0 if scrapped with no proceeds)
    @NotNull(message = "Disposal proceeds is required")
    @DecimalMin(value = "0.0", message = "Disposal proceeds cannot be negative")
    private BigDecimal disposalProceeds;

    // Account the proceeds are received into (e.g. Cash or Bank). Not required if proceeds = 0.
    private Long proceedsAccountId;

    // Gain/Loss on Disposal account. Required only if there is an actual gain or loss.
    private Long gainLossAccountId;

    private String notes;
}
