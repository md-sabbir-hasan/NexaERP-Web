package com.nexaerp.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {


    Optional<Payment> findTopByOrderByIdDesc();
    List<Payment> findByPartyId(Long partyId);
    List<Payment> findByStatus(PaymentStatus status);
    List<Payment> findByPaymentType(PaymentType paymentType);
    List<Payment> findByStatusAndPaymentDateLessThanEqual(PaymentStatus status, LocalDate date);

}
