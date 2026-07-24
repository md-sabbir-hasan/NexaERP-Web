import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { APP_CONFIG } from '../../../core/config/app.config';
import { ApiResponse } from '../../../core/models/api-response.model';
import { Currency, CurrencyConversion } from '../models/currency.model';

@Injectable({
  providedIn: 'root',
})
export class CurrencyService {
  private readonly currencyUrl = `${APP_CONFIG.apiUrl}/currencies`;

  private readonly exchangeRateUrl = `${APP_CONFIG.apiUrl}/exchange-rates`;

  constructor(private http: HttpClient) {}

  getActiveCurrencies(): Observable<ApiResponse<Currency[]>> {
    return this.http.get<ApiResponse<Currency[]>>(`${this.currencyUrl}/active`);
  }

  getBaseCurrency(): Observable<ApiResponse<Currency>> {
    return this.http.get<ApiResponse<Currency>>(`${this.currencyUrl}/base`);
  }

  convert(
    fromCurrency: string,
    toCurrency: string,
    amount: number,
    date: string,
  ): Observable<ApiResponse<CurrencyConversion>> {
    let params = new HttpParams()
      .set('from', fromCurrency)
      .set('to', toCurrency)
      .set('amount', amount.toString());

    if (date) {
      params = params.set('date', date);
    }

    return this.http.get<ApiResponse<CurrencyConversion>>(`${this.exchangeRateUrl}/convert`, {
      params,
    });
  }
}
