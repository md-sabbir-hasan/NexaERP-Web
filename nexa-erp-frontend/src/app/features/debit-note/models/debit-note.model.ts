export type DebitNoteStatus = 'DRAFT' | 'APPROVED' | 'POSTED' | 'CANCELLED';

export type DebitNoteReason =
  | 'PURCHASE_RETURN'
  | 'PRICE_ADJUSTMENT'
  | 'BILL_CORRECTION'
  | 'VAT_ADJUSTMENT'
  | 'QUALITY_REJECTION'
  | 'OTHER';

export type DebitNoteCancelledReason =
  'DUPLICATE' | 'WRONG_VENDOR' | 'WRONG_AMOUNT' | 'WRONG_REFERENCE' | 'OTHER';

export interface DebitNoteItemRequest {
  vendorBillItemId: number;
  quantity: number;
}

export interface DebitNoteRequest {
  vendorBillId: number;
  debitNoteDate: string;
  postingDate: string | null;
  reason: DebitNoteReason;
  reference: string | null;
  notes: string | null;
  items: DebitNoteItemRequest[];
}

export interface DebitNoteItem {
  id: number;

  vendorBillItemId: number;

  expenseAccountId: number;
  expenseAccountName: string;

  description: string;

  quantity: number;
  unitPrice: number;

  discountPercent: number;
  discountAmount: number;

  vatRate: number;
  vatAmount: number;

  tdsRate: number;
  tdsAmount: number;

  subTotal: number;
  lineTotal: number;
  netAdjustment: number;
}

export interface DebitNote {
  id: number;

  debitNoteNumber: string;
  debitNoteDate: string;
  postingDate: string;

  vendorBillId: number;
  vendorBillNumber: string;

  partyId: number;
  partyName: string;

  status: DebitNoteStatus;
  reason: DebitNoteReason;

  reference: string | null;
  notes: string | null;

  subTotal: number;
  discountAmount: number;
  vatAmount: number;
  tdsAmount: number;
  grandTotal: number;
  netAdjustment: number;

  approvedAt: string | null;
  postedAt: string | null;

  cancelledReason: DebitNoteCancelledReason | null;
  cancelledAt: string | null;

  createdAt: string;
  updatedAt: string;

  items: DebitNoteItem[];
}
