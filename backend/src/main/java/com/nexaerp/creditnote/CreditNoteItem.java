package com.nexaerp.creditnote;

import com.nexaerp.invoice.InvoiceItem;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name="credit_note_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreditNoteItem {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="credit_note_id",nullable=false) private CreditNote creditNote;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="invoice_item_id",nullable=false) private InvoiceItem invoiceItem;
    @Column(nullable=false) private String description;
    @Column(nullable=false,precision=19,scale=2) private BigDecimal quantity;
    @Column(nullable=false,precision=19,scale=2) private BigDecimal unitPrice;
    @Column(precision=19,scale=2) private BigDecimal discountPercent;
    @Column(precision=19,scale=2) private BigDecimal discountAmount;
    @Column(precision=19,scale=2) private BigDecimal vatRate;
    @Column(precision=19,scale=2) private BigDecimal vatAmount;
    @Column(precision=19,scale=2) private BigDecimal subTotal;
    @Column(precision=19,scale=2) private BigDecimal lineTotal;
}
