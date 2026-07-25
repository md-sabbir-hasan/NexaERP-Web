package com.nexaerp.vendorbill.dto;

import com.nexaerp.vendorbill.VendorBillCancelledReason;
import com.nexaerp.vendorbill.VendorBillReferenceType;
import com.nexaerp.vendorbill.VendorBillStatus;
import com.nexaerp.vendorbill.VendorBillType;
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
public class VendorBillResponseDto {
    private Long id;

    private String billNumber;

    // Dates
    private LocalDate billDate;
    private LocalDate postingDate;
    private LocalDate dueDate;

    // Vendor own invoice number
    private String vendorBillRef;

    // Vendor details
    private Long partyId;
    private String partyName;

    // Bill classification
    private VendorBillType billType;
    private VendorBillStatus status;

    // Currency
    private String currencyCode;
    private BigDecimal exchangeRate;
    private Integer paymentTerms;

    // Reference to source document
    private VendorBillReferenceType referenceType;
    private String referenceId;

    // Additional info
    private String notes;
    private VendorBillCancelledReason cancelledReason;

    private BigDecimal subTotal;
    private BigDecimal discountAmount;
    private BigDecimal vatAmount;
    private BigDecimal tdsAmount;
    private BigDecimal grandTotal;
    private BigDecimal netPayable;
    private BigDecimal paidAmount;
    private BigDecimal dueAmount;

    // Workflow timestamps
    private LocalDateTime approvedAt;
    private LocalDateTime postedAt;

    // Audit timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Bill line items
    private List<VendorBillItemResponseDto> items;
}
