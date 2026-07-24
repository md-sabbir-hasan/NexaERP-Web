export type DepreciationMethod = 'STRAIGHT_LINE' | 'REDUCING_BALANCE';
export type AssetStatus = 'ACTIVE' | 'FULLY_DEPRECIATED' | 'DISPOSED';

export interface FixedAsset {
  id: number;
  assetCode: string;
  name: string;
  description: string | null;

  assetAccountId: number;
  assetAccountName: string;
  depreciationExpenseAccountId: number;
  depreciationExpenseAccountName: string;
  accumulatedDepreciationAccountId: number;
  accumulatedDepreciationAccountName: string;

  purchaseDate: string;
  purchaseCost: number;
  salvageValue: number;
  usefulLifeYears: number;
  depreciationMethod: DepreciationMethod;
  reducingBalanceRate: number | null;

  accumulatedDepreciation: number;
  bookValue: number;

  status: AssetStatus;
  lastDepreciationDate: string | null;

  disposalDate: string | null;
  disposalProceeds: number | null;
  disposalGainLoss: number | null;

  createdAt: string;
}

export interface FixedAssetRequest {
  name: string;
  description: string | null;
  assetAccountId: number;
  depreciationExpenseAccountId: number;
  accumulatedDepreciationAccountId: number;
  paymentSourceAccountId: number;
  purchaseDate: string;
  purchaseCost: number;
  salvageValue: number;
  usefulLifeYears: number;
  depreciationMethod: DepreciationMethod;
  reducingBalanceRate: number | null;
}

export interface DepreciationEntry {
  id: number;
  fixedAssetId: number;
  assetCode: string;
  assetName: string;
  periodDate: string;
  depreciationAmount: number;
  accumulatedDepreciationAfter: number;
  bookValueAfter: number;
  journalEntryId: number;
}

export interface AssetDisposalRequest {
  disposalDate: string;
  disposalProceeds: number;
  proceedsAccountId: number | null;
  gainLossAccountId: number | null;
  notes: string | null;
}
