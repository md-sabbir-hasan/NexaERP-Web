export interface InvoiceItemOption {
  id: number;
  description: string;
  quantity: number;
  unitPrice: number;
  discountPercent: number;
  vatRate: number;
  lineTotal: number;
}
export interface InvoiceOption {
  id: number;
  invoiceNumber: string;
  invoiceDate: string;
  dueDate: string;
  partyId: number;
  partyName: string;
  status: string;
  grandTotal: number;
  paidAmount: number;
  dueAmount: number;
  items: InvoiceItemOption[];
}
