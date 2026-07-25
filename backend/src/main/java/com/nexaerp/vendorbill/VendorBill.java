package com.nexaerp.vendorbill;

import com.nexaerp.common.BaseEntity;
import com.nexaerp.party.Party;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "vendor_bills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorBill extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String billNumber; // BILL-2025-000001

    @Column(nullable = false)
    private LocalDate billDate;

    private LocalDate postingDate;
    private LocalDate dueDate;
    private String vendorBillRef;

    @ManyToOne
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VendorBillType billType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VendorBillStatus status = VendorBillStatus.DRAFT;

    private String currencyCode = "BDT";
    private BigDecimal exchangeRate = BigDecimal.ONE;
    private Integer paymentTerms = 30;

    @Enumerated(EnumType.STRING)
    private VendorBillReferenceType referenceType = VendorBillReferenceType.MANUAL;

    private String referenceId;
    private String notes;

    @Enumerated(EnumType.STRING)
    private VendorBillCancelledReason cancelledReason;

    // Calculated & Stored
    @Column(precision = 19, scale = 2)
    private BigDecimal subTotal = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal vatAmount = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal tdsAmount = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal grandTotal = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal netPayable = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal dueAmount = BigDecimal.ZERO;

    // Workflow
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private Long postedBy;
    private LocalDateTime postedAt;

    // Audit
//    private Long createdBy;
//    private LocalDateTime createdAt;
//    private Long updatedBy;
//    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "vendorBill", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VendorBillItem> items;

}
