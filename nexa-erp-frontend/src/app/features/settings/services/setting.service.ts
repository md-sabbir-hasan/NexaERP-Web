import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { APP_CONFIG } from '../../../core/config/app.config';
import { ApiResponse } from '../../../core/models/api-response.model';
import { SettingKey, SystemSetting } from '../models/setting.model';

@Injectable({
  providedIn: 'root',
})
export class SettingService {
  private readonly baseUrl = `${APP_CONFIG.apiUrl}/settings`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<ApiResponse<SystemSetting[]>> {
    return this.http.get<ApiResponse<SystemSetting[]>>(this.baseUrl);
  }

  // Backend now accepts a generic { value } body for every SettingKey -
  // for account-mapping keys, value must be the account's numeric ID as a string.
  updateAccountMapping(key: SettingKey, accountId: number): Observable<ApiResponse<SystemSetting>> {
    return this.http.put<ApiResponse<SystemSetting>>(`${this.baseUrl}/${key}`, {
      value: String(accountId),
    });
  }
}
