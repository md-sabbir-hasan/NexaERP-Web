package com.nexaerp.vendorbill;

import com.nexaerp.account.Account;
import com.nexaerp.costcenter.CostCenter;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "vendor_bill_items", indexes = {
        @Index(name = "idx_vendor_bill_items_cost_center", columnList = "cost_center_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorBillItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bill_id", nullable = false)
    private VendorBill vendorBill;

    private Long productId; // nullable, future

    @ManyToOne
    @JoinColumn(name = "expense_account_id", nullable = false)
    private Account expenseAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id")
    private CostCenter costCenter;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(precision = 19, scale = 2)
    private BigDecimal discountPercent = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal vatRate = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal vatAmount = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal tdsRate = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal tdsAmount = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal subTotal = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal lineTotal = BigDecimal.ZERO;
}
