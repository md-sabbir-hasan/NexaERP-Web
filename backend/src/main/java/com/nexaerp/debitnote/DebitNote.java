package com.nexaerp.debitnote;

import com.nexaerp.common.BaseEntity;
import com.nexaerp.party.Party;
import com.nexaerp.vendorbill.VendorBill;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "debit_notes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DebitNote extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "debit_note_number",
            nullable = false,
            unique = true,
            length = 40
    )
    private String debitNoteNumber;

    @Column(
            name = "debit_note_date",
            nullable = false
    )
    private LocalDate debitNoteDate;

    @Column(
            name = "posting_date",
            nullable = false
    )
    private LocalDate postingDate;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "vendor_bill_id",
            nullable = false
    )
    private VendorBill vendorBill;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "party_id",
            nullable = false
    )
    private Party party;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    @Builder.Default
    private DebitNoteStatus status =
            DebitNoteStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 40
    )
    private DebitNoteReason reason;

    @Column(length = 255)
    private String reference;

    @Column(length = 1000)
    private String notes;

    /*
     * Original item amount before discount.
     */
    @Column(
            name = "sub_total",
            nullable = false,
            precision = 19,
            scale = 2
    )
    @Builder.Default
    private BigDecimal subTotal =
            BigDecimal.ZERO;

    /*
     * Total discount adjustment.
     */
    @Column(
            name = "discount_amount",
            nullable = false,
            precision = 19,
            scale = 2
    )
    @Builder.Default
    private BigDecimal discountAmount =
            BigDecimal.ZERO;

    /*
     * Input VAT being reversed.
     */
    @Column(
            name = "vat_amount",
            nullable = false,
            precision = 19,
            scale = 2
    )
    @Builder.Default
    private BigDecimal vatAmount =
            BigDecimal.ZERO;

    /*
     * TDS payable adjustment.
     */
    @Column(
            name = "tds_amount",
            nullable = false,
            precision = 19,
            scale = 2
    )
    @Builder.Default
    private BigDecimal tdsAmount =
            BigDecimal.ZERO;

    /*
     * Purchase amount after discount plus VAT.
     */
    @Column(
            name = "grand_total",
            nullable = false,
            precision = 19,
            scale = 2
    )
    @Builder.Default
    private BigDecimal grandTotal =
            BigDecimal.ZERO;

    /*
     * Actual amount reducing Vendor Bill due.
     *
     * netAdjustment = grandTotal - tdsAmount
     */
    @Column(
            name = "net_adjustment",
            nullable = false,
            precision = 19,
            scale = 2
    )
    @Builder.Default
    private BigDecimal netAdjustment =
            BigDecimal.ZERO;

    /*
     * Workflow information.
     */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @Column(name = "posted_by")
    private Long postedBy;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "cancelled_reason",
            length = 40
    )
    private DebitNoteCancelledReason cancelledReason;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_by")
    private Long cancelledBy;

    @OneToMany(
            mappedBy = "debitNote",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<DebitNoteItem> items =
            new ArrayList<>();
}