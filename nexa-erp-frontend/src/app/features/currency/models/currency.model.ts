export interface Currency {
  id: number;
  code: string;
  name: string;
  symbol: string;
  decimalPlaces: number;
  active: boolean;
  baseCurrency: boolean;
}

export interface CurrencyConversion {
  fromCurrency: string;
  toCurrency: string;
  originalAmount: number;
  exchangeRate: number;
  convertedAmount: number;
  effectiveDate: string;
}
