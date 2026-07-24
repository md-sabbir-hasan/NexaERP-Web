import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { APP_CONFIG } from '../../../core/config/app.config';
import { ApiResponse } from '../../../core/models/api-response.model';
import { CancelledReason, Invoice, InvoiceRequest, InvoiceStatus } from '../models/invoice.model';

@Injectable({
  providedIn: 'root',
})
export class InvoiceService {
  private readonly baseUrl = `${APP_CONFIG.apiUrl}/invoices`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<ApiResponse<Invoice[]>> {
    return this.http.get<ApiResponse<Invoice[]>>(this.baseUrl);
  }

  getById(id: number): Observable<ApiResponse<Invoice>> {
    return this.http.get<ApiResponse<Invoice>>(`${this.baseUrl}/${id}`);
  }

  getByParty(partyId: number): Observable<ApiResponse<Invoice[]>> {
    return this.http.get<ApiResponse<Invoice[]>>(`${this.baseUrl}/party/${partyId}`);
  }

  getByStatus(status: InvoiceStatus): Observable<ApiResponse<Invoice[]>> {
    return this.http.get<ApiResponse<Invoice[]>>(`${this.baseUrl}/status/${status}`);
  }

  create(request: InvoiceRequest): Observable<ApiResponse<Invoice>> {
    return this.http.post<ApiResponse<Invoice>>(this.baseUrl, request);
  }

  update(id: number, request: InvoiceRequest): Observable<ApiResponse<Invoice>> {
    return this.http.put<ApiResponse<Invoice>>(`${this.baseUrl}/${id}`, request);
  }

  post(id: number): Observable<ApiResponse<Invoice>> {
    return this.http.post<ApiResponse<Invoice>>(`${this.baseUrl}/${id}/post`, {});
  }

  cancel(id: number, reason: CancelledReason): Observable<ApiResponse<Invoice>> {
    const params = new HttpParams().set('reason', reason);

    return this.http.post<ApiResponse<Invoice>>(`${this.baseUrl}/${id}/cancel`, {}, { params });
  }
}
