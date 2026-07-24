export type AccountType = 'ASSET' | 'LIABILITY' | 'EQUITY' | 'REVENUE' | 'EXPENSE';

export interface Account {
  id: number;
  code: string;
  name: string;
  description: string | null;
  type: AccountType;
  isActive: boolean;
  isDefault: boolean;
  parentId: number | null;
  parentName: string | null;
  currentBalance: number;
  hasChildren?: boolean;
  children?: Account[];
}

export interface AccountRequest {
  code: string;
  name: string;
  description: string;
  type: AccountType;
  parentId: number | null;
}
