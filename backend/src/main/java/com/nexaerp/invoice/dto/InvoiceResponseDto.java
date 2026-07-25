package com.nexaerp.invoice.dto;

import com.nexaerp.invoice.CancelledReason;
import com.nexaerp.invoice.InvoiceStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceResponseDto {
    private Long id;
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private Long partyId;
    private String partyName;
    private InvoiceStatus status;
    private Integer paymentTerms;
    private String reference;
    private String notes;
    private CancelledReason cancelledReason;
    private Boolean pdfGenerated;
    private Integer printCount;

    // Totals
    private BigDecimal subTotal;
    private BigDecimal discountAmount;
    private BigDecimal vatAmount;
    private BigDecimal grandTotal;
    private BigDecimal paidAmount;
    private BigDecimal dueAmount;

    // Audit
    private LocalDateTime postedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String currencyCode;

    private BigDecimal exchangeRate;

    private List<InvoiceItemResponseDto> items;
}
