import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { APP_CONFIG } from '../../../core/config/app.config';
import { ApiResponse } from '../../../core/models/api-response.model';
import { ExpenseCancelRequest, ExpenseRequest, ExpenseResponse } from '../models/expense.model';

export interface FileUploadResponse {
  fileName: string;
  originalName: string;
  fileUrl: string;
  fileType: string;
  fileSize: number;
  entityType: string;
  entityId: number;
}

@Injectable({
  providedIn: 'root',
})
export class ExpenseService {
  private readonly baseUrl = `${APP_CONFIG.apiUrl}/expenses`;
  private readonly filesUrl = `${APP_CONFIG.apiUrl}/files`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<ApiResponse<ExpenseResponse[]>> {
    return this.http.get<ApiResponse<ExpenseResponse[]>>(this.baseUrl);
  }

  getById(id: number): Observable<ApiResponse<ExpenseResponse>> {
    return this.http.get<ApiResponse<ExpenseResponse>>(`${this.baseUrl}/${id}`);
  }

  create(request: ExpenseRequest): Observable<ApiResponse<ExpenseResponse>> {
    return this.http.post<ApiResponse<ExpenseResponse>>(this.baseUrl, request);
  }

  post(id: number): Observable<ApiResponse<ExpenseResponse>> {
    return this.http.post<ApiResponse<ExpenseResponse>>(`${this.baseUrl}/${id}/post`, {});
  }

  cancel(id: number, request: ExpenseCancelRequest): Observable<ApiResponse<ExpenseResponse>> {
    return this.http.post<ApiResponse<ExpenseResponse>>(`${this.baseUrl}/${id}/cancel`, request);
  }

  attachReceipt(id: number, attachmentUrl: string): Observable<ApiResponse<ExpenseResponse>> {
    return this.http.patch<ApiResponse<ExpenseResponse>>(`${this.baseUrl}/${id}/attachment`, {
      attachmentUrl,
    });
  }

  uploadReceipt(file: File, expenseId: number): Observable<ApiResponse<FileUploadResponse>> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('entityType', 'EXPENSE');
    formData.append('entityId', expenseId.toString());

    return this.http.post<ApiResponse<FileUploadResponse>>(`${this.filesUrl}/upload`, formData);
  }
}