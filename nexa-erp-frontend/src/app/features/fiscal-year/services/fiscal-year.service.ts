import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { APP_CONFIG } from '../../../core/config/app.config';
import { ApiResponse } from '../../../core/models/api-response.model';
import { FiscalYear, FiscalYearRequest } from '../models/fiscal-year.model';

@Injectable({ providedIn: 'root' })
export class FiscalYearService {
  private readonly baseUrl = `${APP_CONFIG.apiUrl}/fiscal-years`;

  constructor(private readonly http: HttpClient) {}

  getAll(): Observable<ApiResponse<FiscalYear[]>> {
    return this.http.get<ApiResponse<FiscalYear[]>>(this.baseUrl);
  }

  getById(id: number): Observable<ApiResponse<FiscalYear>> {
    return this.http.get<ApiResponse<FiscalYear>>(`${this.baseUrl}/${id}`);
  }

  getActive(): Observable<ApiResponse<FiscalYear>> {
    return this.http.get<ApiResponse<FiscalYear>>(`${this.baseUrl}/active`);
  }

  getForDate(date: string): Observable<ApiResponse<FiscalYear>> {
    const params = new HttpParams().set('date', date);
    return this.http.get<ApiResponse<FiscalYear>>(`${this.baseUrl}/for-date`, { params });
  }

  create(request: FiscalYearRequest): Observable<ApiResponse<FiscalYear>> {
    return this.http.post<ApiResponse<FiscalYear>>(this.baseUrl, request);
  }

  update(id: number, request: FiscalYearRequest): Observable<ApiResponse<FiscalYear>> {
    return this.http.put<ApiResponse<FiscalYear>>(`${this.baseUrl}/${id}`, request);
  }

  activate(id: number): Observable<ApiResponse<FiscalYear>> {
    return this.http.patch<ApiResponse<FiscalYear>>(`${this.baseUrl}/${id}/activate`, {});
  }

  close(id: number): Observable<ApiResponse<FiscalYear>> {
    return this.http.patch<ApiResponse<FiscalYear>>(`${this.baseUrl}/${id}/close`, {});
  }

  delete(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/${id}`);
  }
}
