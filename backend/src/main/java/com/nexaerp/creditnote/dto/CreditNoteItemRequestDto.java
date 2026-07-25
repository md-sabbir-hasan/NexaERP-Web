package com.nexaerp.creditnote.dto;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreditNoteItemRequestDto {
    @NotNull private Long invoiceItemId;
    @NotNull @DecimalMin(value="0.01") private BigDecimal quantity;
}
