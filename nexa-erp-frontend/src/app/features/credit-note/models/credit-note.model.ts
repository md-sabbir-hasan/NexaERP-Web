export type CreditNoteStatus = 'DRAFT' | 'APPROVED' | 'POSTED' | 'CANCELLED';
export type CreditNoteReason =
  | 'SALES_RETURN'
  | 'PRICE_ADJUSTMENT'
  | 'POST_INVOICE_DISCOUNT'
  | 'BILLING_ERROR'
  | 'VAT_ADJUSTMENT'
  | 'OTHER';
export type CreditNoteCancelledReason =
  | 'DUPLICATE'
  | 'WRONG_CUSTOMER'
  | 'WRONG_AMOUNT'
  | 'WRONG_REFERENCE'
  | 'OTHER';

export interface CreditNoteItemRequest {
  invoiceItemId: number;
  quantity: number;
}
export interface CreditNoteRequest {
  invoiceId: number;
  creditNoteDate: string;
  postingDate: string | null;
  reason: CreditNoteReason;
  reference: string | null;
  notes: string | null;
  items: CreditNoteItemRequest[];
}
export interface CreditNoteItem {
  id: number;
  invoiceItemId: number;
  description: string;
  quantity: number;
  unitPrice: number;
  discountPercent: number;
  discountAmount: number;
  vatRate: number;
  vatAmount: number;
  subTotal: number;
  lineTotal: number;
}
export interface CreditNote {
  id: number;
  creditNoteNumber: string;
  creditNoteDate: string;
  postingDate: string;
  invoiceId: number;
  invoiceNumber: string;
  partyId: number;
  partyName: string;
  status: CreditNoteStatus;
  reason: CreditNoteReason;
  reference: string | null;
  notes: string | null;
  subTotal: number;
  discountAmount: number;
  vatAmount: number;
  grandTotal: number;
  approvedAt: string | null;
  postedAt: string | null;
  cancelledReason: CreditNoteCancelledReason | null;
  cancelledAt: string | null;
  createdAt: string;
  updatedAt: string;
  items: CreditNoteItem[];
}
