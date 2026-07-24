import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { APP_CONFIG } from '../../../core/config/app.config';
import { ApiResponse } from '../../../core/models/api-response.model';
import {
  AssetDisposalRequest,
  DepreciationEntry,
  FixedAsset,
  FixedAssetRequest,
} from '../models/fixed-asset.model';

@Injectable({
  providedIn: 'root',
})
export class FixedAssetService {
  private readonly baseUrl = `${APP_CONFIG.apiUrl}/fixed-assets`;

  constructor(private http: HttpClient) {}

  create(request: FixedAssetRequest): Observable<ApiResponse<FixedAsset>> {
    return this.http.post<ApiResponse<FixedAsset>>(this.baseUrl, request);
  }

  getById(id: number): Observable<ApiResponse<FixedAsset>> {
    return this.http.get<ApiResponse<FixedAsset>>(`${this.baseUrl}/${id}`);
  }

  getAll(): Observable<ApiResponse<FixedAsset[]>> {
    return this.http.get<ApiResponse<FixedAsset[]>>(this.baseUrl);
  }

  getDepreciationHistory(id: number): Observable<ApiResponse<DepreciationEntry[]>> {
    return this.http.get<ApiResponse<DepreciationEntry[]>>(`${this.baseUrl}/${id}/depreciation-history`);
  }

  runDepreciation(id: number, asOfDate: string): Observable<ApiResponse<DepreciationEntry>> {
    return this.http.post<ApiResponse<DepreciationEntry>>(`${this.baseUrl}/${id}/run-depreciation`, {
      asOfDate,
    });
  }

  runDepreciationForAll(asOfDate: string): Observable<ApiResponse<DepreciationEntry[]>> {
    return this.http.post<ApiResponse<DepreciationEntry[]>>(
      `${this.baseUrl}/run-depreciation-all?asOfDate=${asOfDate}`,
      {},
    );
  }

  dispose(id: number, request: AssetDisposalRequest): Observable<ApiResponse<FixedAsset>> {
    return this.http.post<ApiResponse<FixedAsset>>(`${this.baseUrl}/${id}/dispose`, request);
  }
}
