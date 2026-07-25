package com.nexaerp.banking.repository;


import com.nexaerp.banking.entity.BankAccount;
import com.nexaerp.banking.enums.BankAccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    List<BankAccount> findByIsActive(Boolean isActive);

    List<BankAccount> findByAccountType(BankAccountType accountType);

    Optional<BankAccount> findTopByOrderByIdDesc();

    Optional<BankAccount> findByCoaAccountId(Long coaAccountId);

    @Query("""
        SELECT COALESCE(SUM(a.currentBalance), 0)
        FROM BankAccount a
        WHERE a.isActive = true
    """)
    BigDecimal sumActiveBalances();

    List<BankAccount> findByIsActiveTrueAndCurrentBalanceLessThan(BigDecimal amount);
}
