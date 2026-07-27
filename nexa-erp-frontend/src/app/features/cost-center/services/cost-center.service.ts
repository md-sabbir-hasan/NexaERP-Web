import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { APP_CONFIG } from '../../../core/config/app.config';
import { ApiResponse } from '../../../core/models/api-response.model';
import { CostCenter, CostCenterLookup, CostCenterRequest } from '../models/cost-center.model';

@Injectable({ providedIn: 'root' })
export class CostCenterService {
  private readonly baseUrl = `${APP_CONFIG.apiUrl}/cost-centers`;

  constructor(private readonly http: HttpClient) {}

  getAll(): Observable<ApiResponse<CostCenter[]>> {
    return this.http.get<ApiResponse<CostCenter[]>>(this.baseUrl);
  }

  search(keyword = '', active: boolean | '' = ''): Observable<ApiResponse<CostCenter[]>> {
    let params = new HttpParams();
    if (keyword.trim()) params = params.set('keyword', keyword.trim());
    if (active !== '') params = params.set('active', String(active));
    return this.http.get<ApiResponse<CostCenter[]>>(`${this.baseUrl}/search`, { params });
  }

  lookup(): Observable<ApiResponse<CostCenterLookup[]>> {
    return this.http.get<ApiResponse<CostCenterLookup[]>>(`${this.baseUrl}/lookup`);
  }

  create(request: CostCenterRequest): Observable<ApiResponse<CostCenter>> {
    return this.http.post<ApiResponse<CostCenter>>(this.baseUrl, request);
  }

  update(id: number, request: CostCenterRequest): Observable<ApiResponse<CostCenter>> {
    return this.http.put<ApiResponse<CostCenter>>(`${this.baseUrl}/${id}`, request);
  }

  activate(id: number): Observable<ApiResponse<null>> {
    return this.http.patch<ApiResponse<null>>(`${this.baseUrl}/${id}/activate`, {});
  }

  deactivate(id: number): Observable<ApiResponse<null>> {
    return this.http.patch<ApiResponse<null>>(`${this.baseUrl}/${id}/deactivate`, {});
  }
}
