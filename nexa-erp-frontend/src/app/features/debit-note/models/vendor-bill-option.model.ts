export interface VendorBillItemOption {
  id: number;

  description: string;

  quantity: number;

  unitPrice: number;

  discountPercent: number;

  vatRate: number;

  tdsRate: number;

  lineTotal: number;

  expenseAccountId: number;

  // Recommended
  expenseAccountName: string;

  // Recommended
  subTotal: number;
}

export interface VendorBillOption {
  id: number;
  billNumber: string;
  billDate: string;
  postingDate: string;
  dueDate: string;
  partyId: number;
  partyName: string;
  status: string;
  grandTotal: number;
  netPayable: number;
  paidAmount: number;
  dueAmount: number;
  items: VendorBillItemOption[];
}
