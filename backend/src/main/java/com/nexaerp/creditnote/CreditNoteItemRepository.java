package com.nexaerp.creditnote;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;
public interface CreditNoteItemRepository extends JpaRepository<CreditNoteItem,Long> {
    List<CreditNoteItem> findByCreditNoteId(Long creditNoteId);
    @Query("""
   select coalesce(sum(i.quantity),0) from CreditNoteItem i
   where i.invoiceItem.id=:invoiceItemId
   and i.creditNote.status=com.nexaerp.creditnote.CreditNoteStatus.POSTED
   and (:excludeId is null or i.creditNote.id<>:excludeId)
 """)
    BigDecimal sumPostedQuantity(@Param("invoiceItemId") Long invoiceItemId,@Param("excludeId") Long excludeId);
}
