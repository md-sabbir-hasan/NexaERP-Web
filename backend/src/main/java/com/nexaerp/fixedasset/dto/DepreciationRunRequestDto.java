package com.nexaerp.fixedasset.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DepreciationRunRequestDto {
    @NotNull(message = "As-of date is required")
    private LocalDate asOfDate;
}
