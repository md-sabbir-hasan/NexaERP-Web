import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { APP_CONFIG } from '../../../core/config/app.config';
import { ApiResponse } from '../../../core/models/api-response.model';
import { GlobalSearchResponse } from '../models/global-search.model';

@Injectable({ providedIn: 'root' })
export class GlobalSearchService {
  private readonly baseUrl = `${APP_CONFIG.apiUrl}/global-search`;

  constructor(private readonly http: HttpClient) {}

  search(query: string, limit = 5): Observable<ApiResponse<GlobalSearchResponse>> {
    const params = new HttpParams()
      .set('q', query.trim())
      .set('limit', String(Math.min(Math.max(limit, 1), 10)));
    return this.http.get<ApiResponse<GlobalSearchResponse>>(this.baseUrl, { params });
  }
}
