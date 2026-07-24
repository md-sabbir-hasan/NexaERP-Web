export interface AgingRow {
  partyId: number;
  partyName: string;

  current: number;
  days1to30: number;
  days31to60: number;
  days61to90: number;
  days91Plus: number;

  totalDue: number;
}

export interface AgingResponse {
  asOfDate: string;

  partyType: 'CUSTOMER' | 'VENDOR';

  rows: AgingRow[];

  totalDue: number;
}
