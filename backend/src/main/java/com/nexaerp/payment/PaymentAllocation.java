package com.nexaerp.payment;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_allocations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentAllocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    // Which type of document this allocation points
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentReferenceType referenceType;

    // The ID of that Invoice or VendorBill
    @Column(nullable = false)
    private Long referenceId;

    // How much money from this payment goes to that specific invoice/bill
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal allocatedAmount;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
