package com.nexaerp.currency.entity;

import com.nexaerp.currency.enums.RateSource;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "exchange_rates",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_exchange_rate_currency_date",
                        columnNames = {
                                "from_currency_id",
                                "to_currency_id",
                                "effective_date"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_exchange_rate_lookup",
                        columnList = "from_currency_id,to_currency_id,effective_date"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "from_currency_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_rate_from_currency")
    )
    private Currency fromCurrency;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "to_currency_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_rate_to_currency")
    )
    private Currency toCurrency;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal rate;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RateSource source = RateSource.MANUAL;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (source == null) {
            source = RateSource.MANUAL;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}