package com.nexaerp.invoice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findTopByOrderByIdDesc();
    List<Invoice> findByPartyId(Long partyId);
    List<Invoice> findByStatus(InvoiceStatus status);
    boolean existsByInvoiceNumber(String invoiceNumber);
    // Used for auto (FIFO) payment allocation
// Returns invoices with remaining due amount, oldest due date first
    List<Invoice> findByPartyIdAndDueAmountGreaterThanAndStatusNotOrderByDueDateAsc(
            Long partyId, BigDecimal dueAmount, InvoiceStatus excludeStatus);

    // ==== Dashboard business KPIs ====

    @Query("SELECT COALESCE(SUM(i.dueAmount), 0) FROM Invoice i " +
            "WHERE i.status IN (com.nexaerp.invoice.InvoiceStatus.POSTED, com.nexaerp.invoice.InvoiceStatus.PARTIAL)")
    BigDecimal sumOutstandingReceivable();

    @Query("SELECT COUNT(i) FROM Invoice i " +
            "WHERE i.status IN (com.nexaerp.invoice.InvoiceStatus.POSTED, com.nexaerp.invoice.InvoiceStatus.PARTIAL) " +
            "AND i.dueDate < :asOfDate")
    long countOverdue(@Param("asOfDate") LocalDate asOfDate);

    @Query("SELECT COALESCE(SUM(i.dueAmount), 0) FROM Invoice i " +
            "WHERE i.status IN (com.nexaerp.invoice.InvoiceStatus.POSTED, com.nexaerp.invoice.InvoiceStatus.PARTIAL) " +
            "AND i.dueDate < :asOfDate")
    BigDecimal sumOverdueAmount(@Param("asOfDate") LocalDate asOfDate);

    @Query("SELECT COALESCE(SUM(i.grandTotal), 0) FROM Invoice i " +
            "WHERE i.invoiceDate BETWEEN :from AND :to " +
            "AND i.status <> com.nexaerp.invoice.InvoiceStatus.DRAFT " +
            "AND i.status <> com.nexaerp.invoice.InvoiceStatus.CANCELLED")
    BigDecimal sumGrandTotalBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    List<Invoice> findByStatusAndInvoiceDateLessThanEqual(InvoiceStatus status, LocalDate date);
}

