import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { APP_CONFIG } from '../../../core/config/app.config';
import { ApiResponse } from '../../../core/models/api-response.model';
import { BankAccount, BankAccountRequest, BankAccountType } from '../models/bank-account.model';

@Injectable({
  providedIn: 'root',
})
export class BankAccountService {
  private readonly baseUrl = `${APP_CONFIG.apiUrl}/bank-accounts`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<ApiResponse<BankAccount[]>> {
    return this.http.get<ApiResponse<BankAccount[]>>(this.baseUrl);
  }

  getById(id: number): Observable<ApiResponse<BankAccount>> {
    return this.http.get<ApiResponse<BankAccount>>(`${this.baseUrl}/${id}`);
  }

  getByType(type: BankAccountType): Observable<ApiResponse<BankAccount[]>> {
    return this.http.get<ApiResponse<BankAccount[]>>(`${this.baseUrl}/type/${type}`);
  }

  create(request: BankAccountRequest): Observable<ApiResponse<BankAccount>> {
    return this.http.post<ApiResponse<BankAccount>>(this.baseUrl, request);
  }

  update(id: number, request: BankAccountRequest): Observable<ApiResponse<BankAccount>> {
    return this.http.put<ApiResponse<BankAccount>>(`${this.baseUrl}/${id}`, request);
  }

  deactivate(id: number): Observable<ApiResponse<null>> {
    return this.http.patch<ApiResponse<null>>(`${this.baseUrl}/${id}/deactivate`, {});
  }

  activate(id: number): Observable<ApiResponse<null>> {
    return this.http.patch<ApiResponse<null>>(`${this.baseUrl}/${id}/activate`, {});
  }
}
