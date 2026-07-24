export type BankAccountType = 'CASH' | 'BANK' | 'MOBILE_WALLET';

export type WalletProvider = 'BKASH' | 'NAGAD' | 'ROCKET';

export interface BankAccount {
  id: number;
  accountName: string;
  accountNumber: string | null;
  bankName: string | null;
  branchName: string | null;
  accountType: BankAccountType;
  currency: string;
  openingBalance: number;
  currentBalance: number;
  isActive: boolean;
  notes: string | null;
  mobileNumber: string | null;
  walletProvider: WalletProvider | null;
  coaAccountId: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface BankAccountRequest {
  accountName: string;
  accountNumber: string | null;
  bankName: string | null;
  branchName: string | null;
  accountType: BankAccountType;
  currency: string;
  openingBalance: number;
  notes: string | null;
  mobileNumber: string | null;
  walletProvider: WalletProvider | null;
  coaAccountId: number | null;
}
