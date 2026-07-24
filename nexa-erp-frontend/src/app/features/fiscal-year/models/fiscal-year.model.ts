export type FiscalYearStatus = 'DRAFT' | 'ACTIVE' | 'CLOSED';

export interface FiscalYear {
  id: number;
  name: string;
  startDate: string;
  endDate: string;
  status: FiscalYearStatus;
  description: string | null;
  current: boolean;
  activatedAt: string | null;
  activatedBy: number | null;
  closedAt: string | null;
  closedBy: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface FiscalYearRequest {
  name: string;
  startDate: string;
  endDate: string;
  description: string | null;
}
