import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { APP_CONFIG } from '../../../core/config/app.config';
import { ApiResponse } from '../../../core/models/api-response.model';
import { Role } from '../models/role.model';

export interface RoleRequest {
  name: string;
  description: string;
  permissionIds: number[];
}

@Injectable({
  providedIn: 'root',
})
export class RoleService {
  private readonly baseUrl = `${APP_CONFIG.apiUrl}/roles`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<ApiResponse<Role[]>> {
    return this.http.get<ApiResponse<Role[]>>(this.baseUrl);
  }

  getById(id: number): Observable<ApiResponse<Role>> {
    return this.http.get<ApiResponse<Role>>(`${this.baseUrl}/${id}`);
  }

  create(request: RoleRequest): Observable<ApiResponse<Role>> {
    return this.http.post<ApiResponse<Role>>(this.baseUrl, request);
  }

  update(id: number, request: RoleRequest): Observable<ApiResponse<Role>> {
    return this.http.put<ApiResponse<Role>>(`${this.baseUrl}/${id}`, request);
  }

  assignPermissions(id: number, permissionIds: number[]): Observable<ApiResponse<Role>> {
    return this.http.post<ApiResponse<Role>>(
      `${this.baseUrl}/${id}/permissions/assign`,
      permissionIds,
    );
  }

  removePermissions(id: number, permissionIds: number[]): Observable<ApiResponse<Role>> {
    return this.http.post<ApiResponse<Role>>(
      `${this.baseUrl}/${id}/permissions/remove`,
      permissionIds,
    );
  }
}