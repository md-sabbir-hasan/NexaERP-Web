import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { APP_CONFIG } from '../../../core/config/app.config';
import { ApiResponse } from '../../../core/models/api-response.model';
import { PageResponse } from '../../../core/models/page.model';
import { User, UserRequest, UserStatus } from '../models/user.model';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private readonly baseUrl = `${APP_CONFIG.apiUrl}/users`;

  constructor(private http: HttpClient) {}

  getUsers(
    page = 0,
    size = 10,
    search = '',
    status?: UserStatus,
  ): Observable<ApiResponse<PageResponse<User>>> {
    let params = new HttpParams().set('page', page).set('size', size);

    if (search.trim()) {
      params = params.set('search', search.trim());
    }

    if (status) {
      params = params.set('status', status);
    }

    return this.http.get<ApiResponse<PageResponse<User>>>(this.baseUrl, { params });
  }

  getById(id: number): Observable<ApiResponse<User>> {
    return this.http.get<ApiResponse<User>>(`${this.baseUrl}/${id}`);
  }

  create(request: UserRequest): Observable<ApiResponse<User>> {
    return this.http.post<ApiResponse<User>>(this.baseUrl, request);
  }

  update(id: number, request: UserRequest): Observable<ApiResponse<User>> {
    return this.http.put<ApiResponse<User>>(`${this.baseUrl}/${id}`, request);
  }

  deactivate(id: number): Observable<ApiResponse<null>> {
    return this.http.patch<ApiResponse<null>>(`${this.baseUrl}/${id}/deactivate`, {});
  }

  activate(id: number): Observable<ApiResponse<null>> {
    return this.http.patch<ApiResponse<null>>(`${this.baseUrl}/${id}/activate`, {});
  }
}