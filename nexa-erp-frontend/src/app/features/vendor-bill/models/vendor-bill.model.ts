export type VendorBillStatus = 'DRAFT' | 'APPROVED' | 'POSTED' | 'PARTIAL' | 'PAID' | 'CANCELLED';

export type VendorBillType = 'EXPENSE' | 'PURCHASE' | 'SERVICE' | 'ASSET';

export type VendorBillReferenceType = 'PURCHASE_ORDER' | 'GOODS_RECEIPT' | 'MANUAL';

export type VendorBillCancelledReason = 'VENDOR_REQUESTED' | 'WRONG_ENTRY' | 'DUPLICATE_ENTRY';

export interface BudgetWarning {
  budgetId: number;
  accountId: number;
  accountCode: string;
  accountName: string;
  accountingPeriodId: number;
  accountingPeriodName: string;
  budgetAmount: number;
  actualBeforePosting: number;
  transactionAmount: number;
  projectedActual: number;
  exceededAmount: number;
  message: string;
}

export interface VendorBillItem {
  id: number;
  productId: number | null;
  expenseAccountId: number;
  expenseAccountName: string;
  expenseAccountCode: string;
  costCenterId: number | null;
  costCenterCode: string | null;
  costCenterName: string | null;
  description: string;
  quantity: number;
  unitPrice: number;
  unit: string | null;
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
  attachmentUrl: string | null;
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
  createdBy: number | null;
  approvalFeatureEnabled: boolean;
  activeApprovalId: number | null;
  approvalStatus: 'PENDING' | 'APPROVED' | 'REJECTED' | 'RETURNED' | 'CANCELLED' | null;
  approvalConsumed: boolean | null;
  createdAt: string;
  updatedAt: string;
  items: VendorBillItem[];
  budgetWarnings: BudgetWarning[];
}

export interface VendorBillItemRequest {
  productId: number | null;
  expenseAccountId: number | null;
  costCenterId: number | null;
  description: string;
  quantity: number;
  unitPrice: number;
  unit: string | null;
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
