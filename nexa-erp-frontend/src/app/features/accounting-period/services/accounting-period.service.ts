import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { APP_CONFIG } from '../../../core/config/app.config';
import { ApiResponse } from '../../../core/models/api-response.model';
import { AccountingPeriod, AccountingPeriodRequest } from '../models/accounting-period.model';

@Injectable({ providedIn: 'root' })
export class AccountingPeriodService {
  private readonly baseUrl = `${APP_CONFIG.apiUrl}/accounting-periods`;

  constructor(private readonly http: HttpClient) {}

  getAll(fiscalYearId?: number | null): Observable<ApiResponse<AccountingPeriod[]>> {
    let params = new HttpParams();
    if (fiscalYearId != null) params = params.set('fiscalYearId', fiscalYearId);
    return this.http.get<ApiResponse<AccountingPeriod[]>>(this.baseUrl, { params });
  }

  getById(id: number): Observable<ApiResponse<AccountingPeriod>> {
    return this.http.get<ApiResponse<AccountingPeriod>>(`${this.baseUrl}/${id}`);
  }

  getCurrent(date?: string): Observable<ApiResponse<AccountingPeriod>> {
    let params = new HttpParams();
    if (date) params = params.set('date', date);
    return this.http.get<ApiResponse<AccountingPeriod>>(`${this.baseUrl}/current`, { params });
  }

  create(request: AccountingPeriodRequest): Observable<ApiResponse<AccountingPeriod>> {
    return this.http.post<ApiResponse<AccountingPeriod>>(this.baseUrl, request);
  }

  generate(fiscalYearId: number): Observable<ApiResponse<AccountingPeriod[]>> {
    return this.http.post<ApiResponse<AccountingPeriod[]>>(
      `${this.baseUrl}/generate/${fiscalYearId}`,
      {},
    );
  }

  update(id: number, request: AccountingPeriodRequest): Observable<ApiResponse<AccountingPeriod>> {
    return this.http.put<ApiResponse<AccountingPeriod>>(`${this.baseUrl}/${id}`, request);
  }

  open(id: number, remarks?: string | null): Observable<ApiResponse<AccountingPeriod>> {
    let params = new HttpParams();
    if (remarks?.trim()) params = params.set('remarks', remarks.trim());
    return this.http.patch<ApiResponse<AccountingPeriod>>(`${this.baseUrl}/${id}/open`, {}, { params });
  }

  close(id: number, remarks?: string | null): Observable<ApiResponse<AccountingPeriod>> {
    let params = new HttpParams();
    if (remarks?.trim()) params = params.set('remarks', remarks.trim());
    return this.http.patch<ApiResponse<AccountingPeriod>>(`${this.baseUrl}/${id}/close`, {}, { params });
  }

  delete(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/${id}`);
  }
}
