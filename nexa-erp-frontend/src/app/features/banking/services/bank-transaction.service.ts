import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { APP_CONFIG } from '../../../core/config/app.config';
import { ApiResponse } from '../../../core/models/api-response.model';
import {
  BankTransaction,
  BankTransactionRequest,
  BankTransferRequest,
  BankTransferResponse,
} from '../models/bank-transaction.model';

@Injectable({
  providedIn: 'root',
})
export class BankTransactionService {
  private readonly baseUrl = `${APP_CONFIG.apiUrl}/bank-transactions`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<ApiResponse<BankTransaction[]>> {
    return this.http.get<ApiResponse<BankTransaction[]>>(this.baseUrl);
  }

  getById(id: number): Observable<ApiResponse<BankTransaction>> {
    return this.http.get<ApiResponse<BankTransaction>>(`${this.baseUrl}/${id}`);
  }

  getByAccount(bankAccountId: number): Observable<ApiResponse<BankTransaction[]>> {
    return this.http.get<ApiResponse<BankTransaction[]>>(
      `${this.baseUrl}/account/${bankAccountId}`,
    );
  }

  create(request: BankTransactionRequest): Observable<ApiResponse<BankTransaction>> {
    return this.http.post<ApiResponse<BankTransaction>>(this.baseUrl, request);
  }

  transfer(request: BankTransferRequest): Observable<ApiResponse<BankTransferResponse>> {
    return this.http.post<ApiResponse<BankTransferResponse>>(`${this.baseUrl}/transfer`, request);
  }

  reconcile(id: number): Observable<ApiResponse<BankTransaction>> {
    return this.http.patch<ApiResponse<BankTransaction>>(`${this.baseUrl}/${id}/reconcile`, {});
  }

  unreconcile(id: number): Observable<ApiResponse<BankTransaction>> {
    return this.http.patch<ApiResponse<BankTransaction>>(`${this.baseUrl}/${id}/unreconcile`, {});
  }

  voidTransaction(id: number): Observable<ApiResponse<BankTransaction>> {
    return this.http.patch<ApiResponse<BankTransaction>>(`${this.baseUrl}/${id}/void`, {});
  }
}
