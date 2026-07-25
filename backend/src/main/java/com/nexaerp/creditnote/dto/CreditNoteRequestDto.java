package com.nexaerp.creditnote.dto;
import com.nexaerp.creditnote.CreditNoteReason;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.util.List;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreditNoteRequestDto {
    @NotNull private Long invoiceId;
    @NotNull private LocalDate creditNoteDate;
    private LocalDate postingDate;
    @NotNull private CreditNoteReason reason;
    @Size(max=100) private String reference;
    @Size(max=1000) private String notes;
    @NotEmpty @Valid private List<CreditNoteItemRequestDto> items;
}
