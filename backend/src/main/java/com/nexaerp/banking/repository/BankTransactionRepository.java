package com.nexaerp.banking.repository;


import com.nexaerp.banking.entity.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {

    List<BankTransaction> findByBankAccountId(Long bankAccountId);
    List<BankTransaction> findByBankAccountIdAndTransactionDateBetween(
            Long bankAccountId, LocalDate from, LocalDate to);
    List<BankTransaction> findByReconciled(Boolean reconciled);
    Optional<BankTransaction> findTopByOrderByIdDesc();

    List<BankTransaction> findByReconciliationId(Long reconciliationId);

    // Candidates for matching: not yet reconciled, not voided, dated on/before the statement date
    List<BankTransaction> findByBankAccountIdAndReconciledFalseAndVoidedFalseAndTransactionDateLessThanEqual(
            Long bankAccountId, LocalDate asOfDate);

    // Net book movement (credits - debits) for a bank account up to a given date, ignoring voided entries.
    // openingBalance + this sum = book balance as of that date.
    @Query("SELECT COALESCE(SUM(CASE WHEN t.transactionType = 'CREDIT' THEN t.amount ELSE -t.amount END), 0) " +
            "FROM BankTransaction t " +
            "WHERE t.bankAccount.id = :bankAccountId AND t.voided = false AND t.transactionDate <= :asOfDate")
    BigDecimal sumNetMovementUpTo(@Param("bankAccountId") Long bankAccountId,
                                  @Param("asOfDate") LocalDate asOfDate);


    Optional<BankTransaction> findByReferenceNumber(String referenceNumber);

    List<BankTransaction> findByReconciledFalseAndVoidedFalseAndTransactionDateLessThanEqual(LocalDate date);
}
