import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { APP_CONFIG } from '../../../core/config/app.config';
import { ApiResponse } from '../../../core/models/api-response.model';
import { CancelledReason, Invoice, InvoiceRequest, InvoiceStatus } from '../models/invoice.model';
import { ApprovalRequest } from '../../approval/models/approval.model';

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

  submitForApproval(id: number): Observable<ApiResponse<ApprovalRequest>> {
    return this.http.post<ApiResponse<ApprovalRequest>>(
      `${this.baseUrl}/${id}/submit-approval`,
      {},
    );
  }

  cancel(id: number, reason: CancelledReason): Observable<ApiResponse<Invoice>> {
    const params = new HttpParams().set('reason', reason);

    return this.http.post<ApiResponse<Invoice>>(`${this.baseUrl}/${id}/cancel`, {}, { params });
  }

  uploadAttachment(
    id: number,
    file: File,
  ): Observable<ApiResponse<{ fileUrl: string; originalName: string }>> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post<ApiResponse<{ fileUrl: string; originalName: string }>>(
      `${this.baseUrl}/${id}/attachment`,
      formData,
    );
  }

  downloadPdf(id: number): Observable<Blob> {
    return this.http.get(`${APP_CONFIG.apiUrl}/invoices/${id}/pdf`, {
      responseType: 'blob',
    });
  }
}
