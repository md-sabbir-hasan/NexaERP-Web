package com.nexaerp.banking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConvertLineToAdjustmentRequestDto {
    @NotNull(message = "Contra account is required")
    private Long contraAccountId;

    // Optional override; if blank, the statement line's own description is used
    private String description;
}
