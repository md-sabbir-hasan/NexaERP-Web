package com.nexaerp.invoice.dto;

import com.nexaerp.invoice.CancelledReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceRequestDto {
    @NotNull(message = "Party is required")
    private Long partyId;

    @NotNull(message = "Invoice date is required")
    private LocalDate invoiceDate;

    private Integer paymentTerms;
    private String currencyCode;
    private String reference;
    private String notes;
    private CancelledReason cancelledReason;

    @NotNull(message = "Items are required")
    @Size(min = 1, message = "At least 1 item required")
    private List<InvoiceItemRequestDto> items;
}
