package com.nexaerp.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {


    Optional<Payment> findTopByOrderByIdDesc();
    List<Payment> findByPartyId(Long partyId);
    List<Payment> findByStatus(PaymentStatus status);
    List<Payment> findByPaymentType(PaymentType paymentType);
    Page<Payment> findByPaymentNumberContainingIgnoreCase(String paymentNumber, Pageable pageable);
    List<Payment> findByStatusAndPaymentDateLessThanEqual(PaymentStatus status, LocalDate date);

}
