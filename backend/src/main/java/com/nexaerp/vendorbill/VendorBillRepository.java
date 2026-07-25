package com.nexaerp.vendorbill;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VendorBillRepository extends JpaRepository<VendorBill, Long> {
    Optional<VendorBill> findTopByOrderByIdDesc();
    List<VendorBill> findByPartyId(Long partyId);
    List<VendorBill> findByStatus(VendorBillStatus status);
    List<VendorBill> findByBillType(VendorBillType billType);
    // Used for auto (FIFO) payment allocation
// Returns bills with remaining due amount, oldest due date first
    List<VendorBill> findByPartyIdAndDueAmountGreaterThanAndStatusNotOrderByDueDateAsc(
            Long partyId, BigDecimal dueAmount, VendorBillStatus excludeStatus);

    // ==== Dashboard business KPIs ====

    @Query("SELECT COALESCE(SUM(b.dueAmount), 0) FROM VendorBill b " +
            "WHERE b.status IN (com.nexaerp.vendorbill.VendorBillStatus.POSTED, com.nexaerp.vendorbill.VendorBillStatus.PARTIAL)")
    BigDecimal sumOutstandingPayable();

    @Query("SELECT COUNT(b) FROM VendorBill b " +
            "WHERE b.status IN (com.nexaerp.vendorbill.VendorBillStatus.POSTED, com.nexaerp.vendorbill.VendorBillStatus.PARTIAL) " +
            "AND b.dueDate < :asOfDate")
    long countOverdue(@Param("asOfDate") LocalDate asOfDate);

    @Query("SELECT COALESCE(SUM(b.dueAmount), 0) FROM VendorBill b " +
            "WHERE b.status IN (com.nexaerp.vendorbill.VendorBillStatus.POSTED, com.nexaerp.vendorbill.VendorBillStatus.PARTIAL) " +
            "AND b.dueDate < :asOfDate")
    BigDecimal sumOverdueAmount(@Param("asOfDate") LocalDate asOfDate);

    @Query("SELECT COALESCE(SUM(b.grandTotal), 0) FROM VendorBill b " +
            "WHERE b.billDate BETWEEN :from AND :to " +
            "AND b.status <> com.nexaerp.vendorbill.VendorBillStatus.DRAFT " +
            "AND b.status <> com.nexaerp.vendorbill.VendorBillStatus.CANCELLED")
    BigDecimal sumGrandTotalBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    List<VendorBill> findByStatusAndBillDateLessThanEqual(VendorBillStatus status, LocalDate date);

}
