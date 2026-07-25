package com.nexaerp.fixedasset;

import com.nexaerp.account.Account;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fixed_assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FixedAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String assetCode;

    @Column(nullable = false)
    private String name;

    private String description;

    // GL account this asset's cost sits in (e.g. "Office Equipment")
    @ManyToOne
    @JoinColumn(name = "asset_account_id", nullable = false)
    private Account assetAccount;

    // GL account depreciation expense is charged to (P&L)
    @ManyToOne
    @JoinColumn(name = "depreciation_expense_account_id", nullable = false)
    private Account depreciationExpenseAccount;

    // GL account accumulated depreciation is credited to (contra-asset)
    @ManyToOne
    @JoinColumn(name = "accumulated_depreciation_account_id", nullable = false)
    private Account accumulatedDepreciationAccount;

    @Column(nullable = false)
    private LocalDate purchaseDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal purchaseCost;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal salvageValue;

    @Column(nullable = false)
    private Integer usefulLifeYears;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DepreciationMethod depreciationMethod;

    // Only used for REDUCING_BALANCE, annual rate in percent (e.g. 20 for 20%)
    private BigDecimal reducingBalanceRate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal accumulatedDepreciation = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetStatus status = AssetStatus.ACTIVE;

    // Last date depreciation was run up to (prevents double-charging the same period)
    private LocalDate lastDepreciationDate;

    private LocalDate disposalDate;
    private BigDecimal disposalProceeds;
    private BigDecimal disposalGainLoss;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Depreciable base (the part that is allowed to be written off)
    public BigDecimal depreciableAmount() {
        return purchaseCost.subtract(salvageValue);
    }

    public BigDecimal bookValue() {
        return purchaseCost.subtract(accumulatedDepreciation);
    }
}
