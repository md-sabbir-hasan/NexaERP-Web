import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { APP_CONFIG } from '../../../core/config/app.config';
import { ApiResponse } from '../../../core/models/api-response.model';
import { PageResponse } from '../../../core/models/page.model';
import { AuditLog } from '../models/audit-log.model';

@Injectable({
  providedIn: 'root',
})
export class AuditLogService {
  private readonly baseUrl = `${APP_CONFIG.apiUrl}/audit-logs`;

  constructor(private http: HttpClient) {}

  // All logs for an entity type, e.g. all INVOICE logs
  getEntityLogs(
    entityName: string,
    page = 0,
    size = 20,
  ): Observable<ApiResponse<PageResponse<AuditLog>>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ApiResponse<PageResponse<AuditLog>>>(
      `${this.baseUrl}/entity/${entityName}`,
      { params },
    );
  }

  // Full history of one specific record, e.g. INVOICE #42
  getEntityHistory(
    entityName: string,
    entityId: number,
    page = 0,
    size = 20,
  ): Observable<ApiResponse<PageResponse<AuditLog>>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ApiResponse<PageResponse<AuditLog>>>(
      `${this.baseUrl}/entity/${entityName}/${entityId}`,
      { params },
    );
  }

  // Everything a specific user has done
  getUserActivity(
    userId: number,
    page = 0,
    size = 20,
  ): Observable<ApiResponse<PageResponse<AuditLog>>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ApiResponse<PageResponse<AuditLog>>>(`${this.baseUrl}/user/${userId}`, {
      params,
    });
  }
}
