package com.nexaerp.invoice;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "invoice_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    private Long productId; // nullable, future

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
    private BigDecimal subTotal = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal lineTotal = BigDecimal.ZERO;
}
