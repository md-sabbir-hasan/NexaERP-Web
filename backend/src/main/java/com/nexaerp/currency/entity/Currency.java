package com.nexaerp.currency.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "currencies",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_currency_code",
                        columnNames = "code"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Currency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 3)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 10)
    private String symbol;

    @Column(nullable = false)
    @Builder.Default
    private Integer decimalPlaces = 2;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean baseCurrency = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.code = normalizeCode(this.code);
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        if (this.active == null) {
            this.active = true;
        }

        if (this.baseCurrency == null) {
            this.baseCurrency = false;
        }

        if (this.decimalPlaces == null) {
            this.decimalPlaces = 2;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.code = normalizeCode(this.code);
        this.updatedAt = LocalDateTime.now();
    }

    private String normalizeCode(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}