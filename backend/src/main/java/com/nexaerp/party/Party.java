package com.nexaerp.party;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "parties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Party {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code; // P-0001

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartyType type;

    private Boolean isActive = true;
    private String notes;

    // Company Info
    private String companyName;
    private String contactPerson;
    private String jobPosition;

    // Contact Info
    private String email;

    @Column(nullable = false)
    private String phone;

    private String mobile;

    // Address
    private String street;
    private String city;
    private String state;
    private String country = "Bangladesh";

    // Financial Info
    @Column(precision = 19, scale = 2)
    private BigDecimal creditLimit = BigDecimal.ZERO;

    private Integer paymentTerms = 30;

    @Column(precision = 19, scale = 2)
    private BigDecimal openingBalance = BigDecimal.ZERO;

    private String currency = "BDT";
    private String bankAccountNo;
    private String bankName;

    // Tax Info
    private String bin;
    private String tin;
    private Boolean vatRegistered = false;

    // Document Info
    private String tradeLicenseNo;
    private LocalDate tradeLicenseExpiry;
    private String binCertificateNo;
    private String tinCertificateNo;
    private String nidNo;

    // Document URLs (actual file paths)
    private String tradeLicenseUrl;
    private String binCertificateUrl;
    private String tinCertificateUrl;
    private String nidUrl;
}
