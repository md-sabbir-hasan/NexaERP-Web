export type InvoiceStatus = 'DRAFT' | 'POSTED' | 'PARTIAL' | 'PAID' | 'CANCELLED';

export type CancelledReason = 'CUSTOMER_REQUESTED' | 'WRONG_ENTRY';

export interface InvoiceItem {
  id: number;
  productId: number | null;
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

export interface Invoice {
  id: number;
  invoiceNumber: string;
  invoiceDate: string;
  dueDate: string;
  partyId: number;
  partyName: string;
  status: InvoiceStatus;

  // Currency
  currencyCode: string;
  exchangeRate: number;

  // Base Currency (BDT)
  baseGrandTotal: number;
  basePaidAmount: number;
  baseDueAmount: number;

  paymentTerms: number;
  reference: string | null;
  notes: string | null;
  cancelledReason: CancelledReason | null;
  pdfGenerated: boolean;
  printCount: number;

  subTotal: number;
  discountAmount: number;
  vatAmount: number;
  grandTotal: number;
  paidAmount: number;
  dueAmount: number;

  postedAt: string | null;
  createdAt: string;
  updatedAt: string;

  items: InvoiceItem[];
}

export interface InvoiceItemRequest {
  productId: number | null;
  description: string;
  quantity: number;
  unitPrice: number;
  discountPercent: number;
  vatRate: number;
}

export interface InvoiceRequest {
  partyId: number | null;
  invoiceDate: string;
  paymentTerms: number;
  currencyCode: string;
  reference: string;
  notes: string;
  items: InvoiceItemRequest[];
}
