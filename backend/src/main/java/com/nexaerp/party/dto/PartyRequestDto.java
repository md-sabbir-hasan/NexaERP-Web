package com.nexaerp.party.dto;

import com.nexaerp.party.PartyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PartyRequestDto {
    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Type is required")
    private PartyType type;

    private String notes;

    // Company Info
    private String companyName;
    private String contactPerson;
    private String jobPosition;

    // Contact Info
    private String email;

    @NotBlank(message = "Phone is required")
    private String phone;

    private String mobile;

    // Address
    private String street;
    private String city;
    private String state;
    private String country;

    // Financial Info
    private BigDecimal creditLimit;
    private Integer paymentTerms;
    private BigDecimal openingBalance;
    private String currency;
    private String bankAccountNo;
    private String bankName;

    // Tax Info
    private String bin;
    private String tin;
    private Boolean vatRegistered;

    // Document Info
    private String tradeLicenseNo;
    private LocalDate tradeLicenseExpiry;
    private String binCertificateNo;
    private String tinCertificateNo;
    private String nidNo;
}
