import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { APP_CONFIG } from '../../../core/config/app.config';
import { ApiResponse } from '../../../core/models/api-response.model';
import { PageResponse } from '../../../core/models/page.model';
import { NotificationResponse } from '../models/notification.model';

@Injectable({
  providedIn: 'root',
})
export class NotificationApiService {
  private readonly baseUrl = `${APP_CONFIG.apiUrl}/notifications`;

  constructor(private http: HttpClient) {}

  getNotifications(
    page = 0,
    size = 20,
    unreadOnly = false,
  ): Observable<ApiResponse<PageResponse<NotificationResponse>>> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('unreadOnly', unreadOnly);

    return this.http.get<ApiResponse<PageResponse<NotificationResponse>>>(this.baseUrl, {
      params,
    });
  }

  getUnreadCount(): Observable<ApiResponse<number>> {
    return this.http.get<ApiResponse<number>>(`${this.baseUrl}/unread-count`);
  }

  markAsRead(id: number): Observable<ApiResponse<NotificationResponse>> {
    return this.http.patch<ApiResponse<NotificationResponse>>(
      `${this.baseUrl}/${id}/read`,
      null,
    );
  }

  markAllAsRead(): Observable<ApiResponse<void>> {
    return this.http.patch<ApiResponse<void>>(`${this.baseUrl}/read-all`, null);
  }
}
