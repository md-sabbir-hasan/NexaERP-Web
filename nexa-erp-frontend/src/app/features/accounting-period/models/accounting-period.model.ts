export type AccountingPeriodStatus = 'OPEN' | 'CLOSED';

export interface AccountingPeriod {
  id: number;
  fiscalYearId: number;
  fiscalYearName: string;
  name: string;
  periodNumber: number;
  startDate: string;
  endDate: string;
  status: AccountingPeriodStatus;
  future: boolean;
  current: boolean;
  closedAt: string | null;
  closedBy: number | null;
  remarks: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AccountingPeriodRequest {
  fiscalYearId: number;
  name: string;
  periodNumber: number;
  startDate: string;
  endDate: string;
  remarks: string | null;
}
