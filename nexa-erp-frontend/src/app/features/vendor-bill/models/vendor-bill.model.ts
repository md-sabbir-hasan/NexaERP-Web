export type VendorBillStatus =
  | 'DRAFT'
  | 'APPROVED'
  | 'POSTED'
  | 'PARTIAL'
  | 'PAID'
  | 'CANCELLED';

export type VendorBillType = 'EXPENSE' | 'PURCHASE' | 'SERVICE' | 'ASSET';

export type VendorBillReferenceType =
  | 'PURCHASE_ORDER'
  | 'GOODS_RECEIPT'
  | 'MANUAL';

export type VendorBillCancelledReason =
  | 'VENDOR_REQUESTED'
  | 'WRONG_ENTRY'
  | 'DUPLICATE_ENTRY';

export interface VendorBillItem {
  id: number;
  productId: number | null;
  expenseAccountId: number;
  expenseAccountName: string;
  expenseAccountCode: string;
  costCenterId: number | null;
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
}

export interface VendorBill {
  id: number;
  billNumber: string;
  billDate: string;
  postingDate: string;
  dueDate: string;
  vendorBillRef: string | null;
  partyId: number;
  partyName: string;
  billType: VendorBillType;
  status: VendorBillStatus;
  currencyCode: string;
  exchangeRate: number;
  paymentTerms: number;
  referenceType: VendorBillReferenceType;
  referenceId: string | null;
  notes: string | null;
  cancelledReason: VendorBillCancelledReason | null;
  subTotal: number;
  discountAmount: number;
  vatAmount: number;
  tdsAmount: number;
  grandTotal: number;
  netPayable: number;
  paidAmount: number;
  dueAmount: number;
  approvedAt: string | null;
  postedAt: string | null;
  createdAt: string;
  updatedAt: string;
  items: VendorBillItem[];
}

export interface VendorBillItemRequest {
  productId: number | null;
  expenseAccountId: number | null;
  costCenterId: number | null;
  description: string;
  quantity: number;
  unitPrice: number;
  discountPercent: number;
  vatRate: number;
  tdsRate: number;
}

export interface VendorBillRequest {
  partyId: number | null;
  billDate: string;
  postingDate: string;
  vendorBillRef: string;
  billType: VendorBillType;
  paymentTerms: number;
  currencyCode: string;
  referenceType: VendorBillReferenceType;
  referenceId: string;
  notes: string;
  items: VendorBillItemRequest[];
}