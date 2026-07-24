import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { APP_CONFIG } from '../../../core/config/app.config';
import { ApiResponse } from '../../../core/models/api-response.model';
import { Permission } from '../models/permission.model';

@Injectable({
  providedIn: 'root',
})
export class PermissionService {

  private readonly baseUrl = `${APP_CONFIG.apiUrl}/permissions`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<ApiResponse<Permission[]>> {
    return this.http.get<ApiResponse<Permission[]>>(this.baseUrl);
  }
}