package com.nexaerp.debitnote.dto;

import com.nexaerp.debitnote.DebitNoteCancelledReason;
import com.nexaerp.debitnote.DebitNoteReason;
import com.nexaerp.debitnote.DebitNoteStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DebitNoteResponseDto {

    private Long id;

    private String debitNoteNumber;

    private LocalDate debitNoteDate;

    private LocalDate postingDate;

    private Long vendorBillId;

    private String vendorBillNumber;

    private Long partyId;

    private String partyName;

    private DebitNoteStatus status;

    private DebitNoteReason reason;

    private String reference;

    private String notes;

    private BigDecimal subTotal;

    private BigDecimal discountAmount;

    private BigDecimal vatAmount;

    private BigDecimal tdsAmount;

    private BigDecimal grandTotal;

    private BigDecimal netAdjustment;

    private LocalDateTime approvedAt;

    private LocalDateTime postedAt;

    private LocalDateTime cancelledAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private DebitNoteCancelledReason cancelledReason;

    private List<DebitNoteItemResponseDto> items;
}