package com.nexaerp.debitnote.dto;

import com.nexaerp.debitnote.DebitNoteReason;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DebitNoteRequestDto {
    @NotNull
    private Long vendorBillId;
    @NotNull
    private LocalDate debitNoteDate;
    private LocalDate postingDate;
    @NotNull
    private DebitNoteReason reason;
    @Size(max = 100)
    private String reference;
    @Size(max = 1000)
    private String notes;
    @NotEmpty
    @Valid
    private List<DebitNoteItemRequestDto> items;
}
