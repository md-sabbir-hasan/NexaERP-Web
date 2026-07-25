package com.nexaerp.vendorbill.dto;

import com.nexaerp.vendorbill.VendorBillReferenceType;
import com.nexaerp.vendorbill.VendorBillType;
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
public class VendorBillRequestDto {
    @NotNull(message = "Party is required")
    private Long partyId;

    @NotNull(message = "Bill date is required")
    private LocalDate billDate;

    private LocalDate postingDate;

    private String vendorBillRef;


    @NotNull(message = "Bill type is required")
    private VendorBillType billType;

    private Integer paymentTerms;

    private String currencyCode;

    private VendorBillReferenceType referenceType;

    private String referenceId;

    private String notes;

    @NotNull(message = "Items are required")
    @Size(min = 1, message = "At least 1 item required")
    private List<VendorBillItemRequestDto> items;
}
