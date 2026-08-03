package com.nexaerp.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") Long id);


    Optional<Payment> findTopByOrderByIdDesc();
    List<Payment> findByPartyId(Long partyId);
    List<Payment> findByStatus(PaymentStatus status);
    List<Payment> findByPaymentType(PaymentType paymentType);
    Page<Payment> findByPaymentNumberContainingIgnoreCase(String paymentNumber, Pageable pageable);
    List<Payment> findByStatusAndPaymentDateLessThanEqual(PaymentStatus status, LocalDate date);

}
