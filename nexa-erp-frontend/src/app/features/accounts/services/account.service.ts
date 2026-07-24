import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { APP_CONFIG } from '../../../core/config/app.config';
import { ApiResponse } from '../../../core/models/api-response.model';
import { Account, AccountRequest, AccountType } from '../models/account.model';

@Injectable({
  providedIn: 'root',
})
export class AccountService {
  private readonly baseUrl = `${APP_CONFIG.apiUrl}/accounts`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<ApiResponse<Account[]>> {
    return this.http.get<ApiResponse<Account[]>>(this.baseUrl);
  }

  getTree(): Observable<ApiResponse<Account[]>> {
    return this.http.get<ApiResponse<Account[]>>(`${this.baseUrl}/tree`);
  }

  getById(id: number): Observable<ApiResponse<Account>> {
    return this.http.get<ApiResponse<Account>>(`${this.baseUrl}/${id}`);
  }

  getByType(type: AccountType): Observable<ApiResponse<Account[]>> {
    return this.http.get<ApiResponse<Account[]>>(`${this.baseUrl}/type/${type}`);
  }

  search(
    keyword = '',
    type?: AccountType | '',
    active?: boolean | '',
  ): Observable<ApiResponse<Account[]>> {
    let params = new HttpParams();

    if (keyword.trim()) {
      params = params.set('keyword', keyword.trim());
    }

    if (type) {
      params = params.set('type', type);
    }

    if (active !== '') {
      params = params.set('active', String(active));
    }

    return this.http.get<ApiResponse<Account[]>>(`${this.baseUrl}/search`, { params });
  }

  create(request: AccountRequest): Observable<ApiResponse<Account>> {
    return this.http.post<ApiResponse<Account>>(this.baseUrl, request);
  }

  update(id: number, request: AccountRequest): Observable<ApiResponse<Account>> {
    return this.http.put<ApiResponse<Account>>(`${this.baseUrl}/${id}`, request);
  }

  activate(id: number): Observable<ApiResponse<null>> {
    return this.http.patch<ApiResponse<null>>(`${this.baseUrl}/${id}/activate`, {});
  }

  deactivate(id: number): Observable<ApiResponse<null>> {
    return this.http.patch<ApiResponse<null>>(`${this.baseUrl}/${id}/deactivate`, {});
  }
}
