package com.nexaerp.debitnote;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface DebitNoteItemRepository extends JpaRepository<DebitNoteItem, Long> {
    List<DebitNoteItem> findByDebitNoteId(Long debitNoteId);

    @Query("""
              select coalesce(sum(i.quantity),0) from DebitNoteItem i
              where i.vendorBillItem.id=:vendorBillItemId
              and i.debitNote.status=com.nexaerp.debitnote.DebitNoteStatus.POSTED
              and (:excludeId is null or i.debitNote.id<>:excludeId)
            """)
    BigDecimal sumPostedQuantity(@Param("vendorBillItemId") Long vendorBillItemId, @Param("excludeId") Long excludeId);
}
