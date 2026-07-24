import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { APP_CONFIG } from '../../../core/config/app.config';
import { ApiResponse } from '../../../core/models/api-response.model';
import {
  RecurringExpenseTemplateRequest,
  RecurringExpenseTemplateResponse,
} from '../models/recurring-expense.model';

@Injectable({
  providedIn: 'root',
})
export class RecurringExpenseService {
  private readonly baseUrl = `${APP_CONFIG.apiUrl}/recurring-expenses`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<ApiResponse<RecurringExpenseTemplateResponse[]>> {
    return this.http.get<ApiResponse<RecurringExpenseTemplateResponse[]>>(this.baseUrl);
  }

  getById(id: number): Observable<ApiResponse<RecurringExpenseTemplateResponse>> {
    return this.http.get<ApiResponse<RecurringExpenseTemplateResponse>>(`${this.baseUrl}/${id}`);
  }

  create(request: RecurringExpenseTemplateRequest): Observable<ApiResponse<RecurringExpenseTemplateResponse>> {
    return this.http.post<ApiResponse<RecurringExpenseTemplateResponse>>(this.baseUrl, request);
  }

  update(
    id: number,
    request: RecurringExpenseTemplateRequest,
  ): Observable<ApiResponse<RecurringExpenseTemplateResponse>> {
    return this.http.put<ApiResponse<RecurringExpenseTemplateResponse>>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/${id}`);
  }

  pause(id: number): Observable<ApiResponse<RecurringExpenseTemplateResponse>> {
    return this.http.post<ApiResponse<RecurringExpenseTemplateResponse>>(`${this.baseUrl}/${id}/pause`, {});
  }

  resume(id: number): Observable<ApiResponse<RecurringExpenseTemplateResponse>> {
    return this.http.post<ApiResponse<RecurringExpenseTemplateResponse>>(`${this.baseUrl}/${id}/resume`, {});
  }

  runNow(id: number): Observable<ApiResponse<RecurringExpenseTemplateResponse>> {
    return this.http.post<ApiResponse<RecurringExpenseTemplateResponse>>(`${this.baseUrl}/${id}/run-now`, {});
  }
}