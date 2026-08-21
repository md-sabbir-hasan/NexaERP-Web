import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { APP_CONFIG } from '../../../core/config/app.config';
import { ApiResponse } from '../../../core/models/api-response.model';
import { ApprovalRequest } from '../../approval/models/approval.model';
import {
  VendorBill,
  VendorBillCancelledReason,
  VendorBillRequest,
  VendorBillStatus,
  VendorBillType,
} from '../models/vendor-bill.model';

@Injectable({
  providedIn: 'root',
})
export class VendorBillService {
  private readonly baseUrl = `${APP_CONFIG.apiUrl}/vendor-bills`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<ApiResponse<VendorBill[]>> {
    return this.http.get<ApiResponse<VendorBill[]>>(this.baseUrl);
  }

  getById(id: number): Observable<ApiResponse<VendorBill>> {
    return this.http.get<ApiResponse<VendorBill>>(`${this.baseUrl}/${id}`);
  }

  getByParty(partyId: number): Observable<ApiResponse<VendorBill[]>> {
    return this.http.get<ApiResponse<VendorBill[]>>(`${this.baseUrl}/party/${partyId}`);
  }

  getByStatus(status: VendorBillStatus): Observable<ApiResponse<VendorBill[]>> {
    return this.http.get<ApiResponse<VendorBill[]>>(`${this.baseUrl}/status/${status}`);
  }

  getByBillType(billType: VendorBillType): Observable<ApiResponse<VendorBill[]>> {
    return this.http.get<ApiResponse<VendorBill[]>>(`${this.baseUrl}/type/${billType}`);
  }

  create(request: VendorBillRequest): Observable<ApiResponse<VendorBill>> {
    return this.http.post<ApiResponse<VendorBill>>(this.baseUrl, request);
  }

  update(id: number, request: VendorBillRequest): Observable<ApiResponse<VendorBill>> {
    return this.http.put<ApiResponse<VendorBill>>(`${this.baseUrl}/${id}`, request);
  }

  approve(id: number): Observable<ApiResponse<VendorBill>> {
    return this.http.post<ApiResponse<VendorBill>>(`${this.baseUrl}/${id}/approve`, {});
  }

  submitForApproval(id: number): Observable<ApiResponse<ApprovalRequest>> {
    return this.http.post<ApiResponse<ApprovalRequest>>(
      `${this.baseUrl}/${id}/submit-approval`,
      {},
    );
  }

  post(id: number): Observable<ApiResponse<VendorBill>> {
    return this.http.post<ApiResponse<VendorBill>>(`${this.baseUrl}/${id}/post`, {});
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

  cancel(id: number, reason: VendorBillCancelledReason): Observable<ApiResponse<VendorBill>> {
    const params = new HttpParams().set('reason', reason);

    return this.http.post<ApiResponse<VendorBill>>(`${this.baseUrl}/${id}/cancel`, {}, { params });
  }

  downloadPdf(id: number): Observable<Blob> {
    return this.http.get(`${APP_CONFIG.apiUrl}/vendor-bills/${id}/pdf`, {
      responseType: 'blob',
    });
  }
}
