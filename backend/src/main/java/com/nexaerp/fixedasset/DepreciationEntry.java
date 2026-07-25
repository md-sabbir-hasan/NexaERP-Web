package com.nexaerp.fixedasset;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "depreciation_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepreciationEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "fixed_asset_id", nullable = false)
    private FixedAsset fixedAsset;

    // The date depreciation was calculated as of (period end)
    @Column(nullable = false)
    private LocalDate periodDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal depreciationAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal accumulatedDepreciationAfter;

    @Column(precision = 19, scale = 2)
    private BigDecimal bookValueAfter;

    private Long journalEntryId;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
