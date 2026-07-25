package com.nexaerp.banking.repository;


import com.nexaerp.banking.entity.BankStatementLine;
import com.nexaerp.banking.enums.StatementLineStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankStatementLineRepository extends JpaRepository<BankStatementLine, Long> {

    List<BankStatementLine> findByReconciliationIdOrderByLineDateAsc(Long reconciliationId);

    List<BankStatementLine> findByReconciliationIdAndStatus(Long reconciliationId, StatementLineStatus status);
}
