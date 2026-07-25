export type GlobalSearchResultType =
  | 'INVOICE'
  | 'VENDOR_BILL'
  | 'PAYMENT'
  | 'PARTY'
  | 'ACCOUNT'
  | 'JOURNAL_ENTRY';

export interface GlobalSearchResult {
  id: number;
  type: GlobalSearchResultType;
  title: string;
  subtitle: string;
  status: string;
}

export interface GlobalSearchGroup {
  type: GlobalSearchResultType;
  results: GlobalSearchResult[];
}

export interface GlobalSearchResponse {
  query: string;
  groups: GlobalSearchGroup[];
}
