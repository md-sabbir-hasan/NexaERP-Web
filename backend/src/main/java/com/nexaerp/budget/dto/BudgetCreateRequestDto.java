package com.nexaerp.budget.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BudgetCreateRequestDto {

    @NotNull(message = "Fiscal year is required")
    private Long fiscalYearId;

    @NotBlank(message = "Name is required")
    private String name;

    private String description;
}