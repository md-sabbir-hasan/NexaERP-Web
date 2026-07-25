package com.nexaerp.banking.repository;


import com.nexaerp.banking.entity.BankReconciliation;
import com.nexaerp.banking.enums.ReconciliationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankReconciliationRepository extends JpaRepository<BankReconciliation, Long> {

    List<BankReconciliation> findByBankAccountIdOrderByStatementDateDesc(Long bankAccountId);

    Optional<BankReconciliation> findByBankAccountIdAndStatus(
            Long bankAccountId, ReconciliationStatus status);
}
