package com.nexaerp.party.dto;

import com.nexaerp.party.PartyType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartyResponseDto {
    private Long id;
    private String code;
    private String name;
    private PartyType type;
    private Boolean isActive;
    private String notes;

    // Company Info
    private String companyName;
    private String contactPerson;
    private String jobPosition;

    // Contact Info
    private String email;
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


    //doc
    private String tradeLicenseUrl;
    private String binCertificateUrl;
    private String tinCertificateUrl;
    private String nidUrl;
}
