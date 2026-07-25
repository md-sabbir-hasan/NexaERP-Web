package com.nexaerp.debitnote.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DebitNoteItemRequestDto {
    @NotNull
    private Long vendorBillItemId;
    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal quantity;
}
