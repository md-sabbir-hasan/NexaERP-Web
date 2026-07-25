package com.nexaerp.debitnote;

import com.nexaerp.account.Account;
import com.nexaerp.vendorbill.VendorBillItem;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "debit_note_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DebitNoteItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "debit_note_id",
            nullable = false
    )
    private DebitNote debitNote;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "vendor_bill_item_id",
            nullable = false
    )
    private VendorBillItem vendorBillItem;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "expense_account_id",
            nullable = false
    )
    private Account expenseAccount;

    @Column(
            nullable = false,
            length = 500
    )
    private String description;

    @Column(
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal quantity;

    @Column(
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal unitPrice;

    @Column(
            name = "discount_percent",
            nullable = false,
            precision = 19,
            scale = 2
    )
    @Builder.Default
    private BigDecimal discountPercent =
            BigDecimal.ZERO;

    @Column(
            name = "discount_amount",
            nullable = false,
            precision = 19,
            scale = 2
    )
    @Builder.Default
    private BigDecimal discountAmount =
            BigDecimal.ZERO;

    @Column(
            name = "vat_rate",
            nullable = false,
            precision = 19,
            scale = 2
    )
    @Builder.Default
    private BigDecimal vatRate =
            BigDecimal.ZERO;

    @Column(
            name = "vat_amount",
            nullable = false,
            precision = 19,
            scale = 2
    )
    @Builder.Default
    private BigDecimal vatAmount =
            BigDecimal.ZERO;

    @Column(
            name = "tds_rate",
            nullable = false,
            precision = 19,
            scale = 2
    )
    @Builder.Default
    private BigDecimal tdsRate =
            BigDecimal.ZERO;

    @Column(
            name = "tds_amount",
            nullable = false,
            precision = 19,
            scale = 2
    )
    @Builder.Default
    private BigDecimal tdsAmount =
            BigDecimal.ZERO;

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
     * Purchase value after discount + VAT.
     */
    @Column(
            name = "line_total",
            nullable = false,
            precision = 19,
            scale = 2
    )
    @Builder.Default
    private BigDecimal lineTotal =
            BigDecimal.ZERO;

    /*
     * Actual Vendor Bill due reduction.
     *
     * netAdjustment = lineTotal - tdsAmount
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
}