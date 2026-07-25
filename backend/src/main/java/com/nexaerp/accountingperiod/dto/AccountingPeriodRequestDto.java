package com.nexaerp.accountingperiod.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountingPeriodRequestDto {

    @NotNull(message = "Fiscal year is required")
    private Long fiscalYearId;

    @NotBlank(message = "Period name is required")
    @Size(max = 100, message = "Period name cannot exceed 100 characters")
    private String name;

    @NotNull(message = "Period number is required")
    @Min(value = 1, message = "Period number must be at least 1")
    @Max(value = 99, message = "Period number cannot exceed 99")
    private Integer periodNumber;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @Size(max = 500, message = "Remarks cannot exceed 500 characters")
    private String remarks;
}
