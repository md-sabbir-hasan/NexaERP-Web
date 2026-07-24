export type PartyType = 'CUSTOMER' | 'VENDOR' | 'BOTH';

export interface Party {
  id: number;
  code: string;
  name: string;
  type: PartyType;
  isActive: boolean;
  notes: string | null;

  companyName: string | null;
  contactPerson: string | null;
  jobPosition: string | null;

  email: string | null;
  phone: string;
  mobile: string | null;

  street: string | null;
  city: string | null;
  state: string | null;
  country: string | null;

  creditLimit: number;
  paymentTerms: number;
  openingBalance: number;
  currency: string;
  bankAccountNo: string | null;
  bankName: string | null;

  bin: string | null;
  tin: string | null;
  vatRegistered: boolean;

  tradeLicenseNo: string | null;
  tradeLicenseExpiry: string | null;
  binCertificateNo: string | null;
  tinCertificateNo: string | null;
  nidNo: string | null;

  tradeLicenseUrl: string | null;
  binCertificateUrl: string | null;
  tinCertificateUrl: string | null;
  nidUrl: string | null;
}