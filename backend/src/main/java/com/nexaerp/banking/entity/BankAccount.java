package com.nexaerp.banking.entity;


import com.nexaerp.banking.enums.BankAccountType;
import com.nexaerp.banking.enums.WalletProvider;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String accountName;

    private String accountNumber;
    private String bankName;
    private String branchName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BankAccountType accountType;

    private String currency = "BDT";

    @Column(precision = 19, scale = 2)
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal currentBalance = BigDecimal.ZERO;

    private Boolean isActive = true;
    private String notes;

    // Mobile Wallet specific
    private String mobileNumber;

    @Enumerated(EnumType.STRING)
    private WalletProvider walletProvider;

    // Links to COA account for Journal Entry
    // ei bank account ta COA te kno account er sathe linked
    private Long coaAccountId;

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
}
