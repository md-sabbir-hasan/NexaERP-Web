import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { APP_CONFIG } from '../../../core/config/app.config';
import { ApiResponse } from '../../../core/models/api-response.model';
import {
  BudgetCreateRequest,
  BudgetLineRequest,
  BudgetLineResponse,
  BudgetResponse,
  BudgetUpdateRequest,
  BudgetVarianceResponse,
} from '../models/budget.model';

@Injectable({
  providedIn: 'root',
})
export class BudgetService {
  private readonly baseUrl = `${APP_CONFIG.apiUrl}/budgets`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<ApiResponse<BudgetResponse[]>> {
    return this.http.get<ApiResponse<BudgetResponse[]>>(this.baseUrl);
  }

  getById(id: number): Observable<ApiResponse<BudgetResponse>> {
    return this.http.get<ApiResponse<BudgetResponse>>(`${this.baseUrl}/${id}`);
  }

  getByFiscalYear(fiscalYearId: number): Observable<ApiResponse<BudgetResponse[]>> {
    return this.http.get<ApiResponse<BudgetResponse[]>>(`${this.baseUrl}/fiscal-year/${fiscalYearId}`);
  }

  create(request: BudgetCreateRequest): Observable<ApiResponse<BudgetResponse>> {
    return this.http.post<ApiResponse<BudgetResponse>>(this.baseUrl, request);
  }

  update(id: number, request: BudgetUpdateRequest): Observable<ApiResponse<BudgetResponse>> {
    return this.http.put<ApiResponse<BudgetResponse>>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/${id}`);
  }

  addLine(budgetId: number, request: BudgetLineRequest): Observable<ApiResponse<BudgetLineResponse>> {
    return this.http.post<ApiResponse<BudgetLineResponse>>(`${this.baseUrl}/${budgetId}/lines`, request);
  }

  updateLine(
    budgetId: number,
    lineId: number,
    request: BudgetLineRequest,
  ): Observable<ApiResponse<BudgetLineResponse>> {
    return this.http.put<ApiResponse<BudgetLineResponse>>(
      `${this.baseUrl}/${budgetId}/lines/${lineId}`,
      request,
    );
  }

  deleteLine(budgetId: number, lineId: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/${budgetId}/lines/${lineId}`);
  }

  activate(id: number): Observable<ApiResponse<BudgetResponse>> {
    return this.http.post<ApiResponse<BudgetResponse>>(`${this.baseUrl}/${id}/activate`, {});
  }

  close(id: number): Observable<ApiResponse<BudgetResponse>> {
    return this.http.post<ApiResponse<BudgetResponse>>(`${this.baseUrl}/${id}/close`, {});
  }

  getVariance(
    id: number,
    params?: { periodId?: number; fromDate?: string; toDate?: string },
  ): Observable<ApiResponse<BudgetVarianceResponse>> {
    let httpParams = new HttpParams();
    if (params?.periodId) httpParams = httpParams.set('periodId', params.periodId);
    if (params?.fromDate) httpParams = httpParams.set('fromDate', params.fromDate);
    if (params?.toDate) httpParams = httpParams.set('toDate', params.toDate);

    return this.http.get<ApiResponse<BudgetVarianceResponse>>(`${this.baseUrl}/${id}/variance`, {
      params: httpParams,
    });
  }
}