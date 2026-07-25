package com.nexaerp.creditnote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface CreditNoteRepository extends JpaRepository<CreditNote,Long> {
    Optional<CreditNote> findTopByOrderByIdDesc();
    List<CreditNote> findByInvoiceIdOrderByIdDesc(Long invoiceId);
    List<CreditNote> findByPartyIdOrderByIdDesc(Long partyId);
}
