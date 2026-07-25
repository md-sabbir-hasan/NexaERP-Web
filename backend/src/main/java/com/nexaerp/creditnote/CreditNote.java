package com.nexaerp.creditnote;

import com.nexaerp.common.BaseEntity;
import com.nexaerp.invoice.Invoice;
import com.nexaerp.party.Party;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="credit_notes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreditNote extends BaseEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,unique=true) private String creditNoteNumber;
    @Column(nullable=false) private LocalDate creditNoteDate;
    @Column(nullable=false) private LocalDate postingDate;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="invoice_id",nullable=false) private Invoice invoice;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="party_id",nullable=false) private Party party;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private CreditNoteStatus status;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private CreditNoteReason reason;
    private String reference;
    @Column(length=1000) private String notes;
    @Column(precision=19,scale=2) private BigDecimal subTotal;
    @Column(precision=19,scale=2) private BigDecimal discountAmount;
    @Column(precision=19,scale=2) private BigDecimal vatAmount;
    @Column(precision=19,scale=2) private BigDecimal grandTotal;
    private LocalDateTime approvedAt;
    private Long approvedBy;
    private LocalDateTime postedAt;
    private Long postedBy;
    @Enumerated(EnumType.STRING) private CreditNoteCancelledReason cancelledReason;
    private LocalDateTime cancelledAt;
    @OneToMany(mappedBy="creditNote",cascade=CascadeType.ALL,orphanRemoval=true)
    @Builder.Default private List<CreditNoteItem> items=new ArrayList<>();
}
