package com.nexaerp.debitnote;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface DebitNoteRepository extends JpaRepository<DebitNote, Long> {
    Optional<DebitNote> findTopByOrderByIdDesc();

    List<DebitNote> findByVendorBillIdOrderByIdDesc(Long vendorBillId);

    List<DebitNote> findByPartyIdOrderByIdDesc(Long partyId);
}
