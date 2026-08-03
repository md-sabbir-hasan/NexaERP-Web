package com.nexaerp.invoice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Invoice i where i.id = :id")
    Optional<Invoice> findByIdForUpdate(@Param("id") Long id);
    Optional<Invoice> findTopByOrderByIdDesc();
    List<Invoice> findByPartyId(Long partyId);
    List<Invoice> findByStatus(InvoiceStatus status);
    Page<Invoice> findByInvoiceNumberContainingIgnoreCase(String invoiceNumber, Pageable pageable);
    boolean existsByInvoiceNumber(String invoiceNumber);
    // Used for auto (FIFO) payment allocation
// Returns invoices with remaining due amount, oldest due date first
    List<Invoice> findByPartyIdAndDueAmountGreaterThanAndStatusNotOrderByDueDateAsc(
            Long partyId, BigDecimal dueAmount, InvoiceStatus excludeStatus);
    List<Invoice> findByPartyIdAndDueAmountGreaterThanAndStatusInOrderByDueDateAsc(
            Long partyId, BigDecimal dueAmount, List<InvoiceStatus> statuses);

    // ==== Dashboard business KPIs ====

    @Query("SELECT COALESCE(SUM(i.dueAmount), 0) FROM Invoice i " +
            "WHERE i.status IN (com.nexaerp.invoice.InvoiceStatus.POSTED, com.nexaerp.invoice.InvoiceStatus.PARTIAL)")
    BigDecimal sumOutstandingReceivable();

    @Query("SELECT COUNT(i) FROM Invoice i " +
            "WHERE i.status IN (com.nexaerp.invoice.InvoiceStatus.POSTED, com.nexaerp.invoice.InvoiceStatus.PARTIAL) " +
            "AND i.dueDate < :asOfDate AND i.dueAmount > 0")
    long countOverdue(@Param("asOfDate") LocalDate asOfDate);

    @Query("SELECT COALESCE(SUM(i.dueAmount), 0) FROM Invoice i " +
            "WHERE i.status IN (com.nexaerp.invoice.InvoiceStatus.POSTED, com.nexaerp.invoice.InvoiceStatus.PARTIAL) " +
            "AND i.dueDate < :asOfDate AND i.dueAmount > 0")
    BigDecimal sumOverdueAmount(@Param("asOfDate") LocalDate asOfDate);

    @Query("SELECT COALESCE(SUM(i.grandTotal), 0) FROM Invoice i " +
            "WHERE i.invoiceDate BETWEEN :from AND :to " +
            "AND i.status <> com.nexaerp.invoice.InvoiceStatus.DRAFT " +
            "AND i.status <> com.nexaerp.invoice.InvoiceStatus.CANCELLED")
    BigDecimal sumGrandTotalBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    List<Invoice> findByStatusAndInvoiceDateLessThanEqual(InvoiceStatus status, LocalDate date);

    Page<Invoice> findByStatusInAndDueDateBeforeAndDueAmountGreaterThan(
            List<InvoiceStatus> statuses,
            LocalDate dueDate,
            BigDecimal dueAmount,
            Pageable pageable
    );
}

