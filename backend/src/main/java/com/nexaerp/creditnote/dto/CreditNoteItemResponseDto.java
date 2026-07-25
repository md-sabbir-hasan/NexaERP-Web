package com.nexaerp.creditnote.dto;
import lombok.*;
import java.math.BigDecimal;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreditNoteItemResponseDto {
    private Long id; private Long invoiceItemId; private String description;
    private BigDecimal quantity, unitPrice, discountPercent, discountAmount, vatRate, vatAmount, subTotal, lineTotal;
}
