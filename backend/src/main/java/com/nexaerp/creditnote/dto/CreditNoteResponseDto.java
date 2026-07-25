package com.nexaerp.creditnote.dto;
import com.nexaerp.creditnote.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreditNoteResponseDto {
    private Long id; private String creditNoteNumber; private LocalDate creditNoteDate, postingDate;
    private Long invoiceId; private String invoiceNumber; private Long partyId; private String partyName;
    private CreditNoteStatus status; private CreditNoteReason reason; private String reference, notes;
    private BigDecimal subTotal, discountAmount, vatAmount, grandTotal;
    private LocalDateTime approvedAt, postedAt, cancelledAt, createdAt, updatedAt;
    private CreditNoteCancelledReason cancelledReason; private List<CreditNoteItemResponseDto> items;
}
