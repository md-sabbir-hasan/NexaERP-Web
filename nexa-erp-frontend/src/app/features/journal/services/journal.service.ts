import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { APP_CONFIG } from '../../../core/config/app.config';
import { ApiResponse } from '../../../core/models/api-response.model';
import { JournalEntry, JournalEntryRequest } from '../models/journal.model';

@Injectable({
  providedIn: 'root',
})
export class JournalService {
  private readonly baseUrl = `${APP_CONFIG.apiUrl}/journals`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<ApiResponse<JournalEntry[]>> {
    return this.http.get<ApiResponse<JournalEntry[]>>(this.baseUrl);
  }

  getById(id: number): Observable<ApiResponse<JournalEntry>> {
    return this.http.get<ApiResponse<JournalEntry>>(`${this.baseUrl}/${id}`);
  }

  create(request: JournalEntryRequest): Observable<ApiResponse<JournalEntry>> {
    return this.http.post<ApiResponse<JournalEntry>>(this.baseUrl, request);
  }

  update(id: number, request: JournalEntryRequest): Observable<ApiResponse<JournalEntry>> {
    return this.http.put<ApiResponse<JournalEntry>>(`${this.baseUrl}/${id}`, request);
  }

  post(id: number): Observable<ApiResponse<JournalEntry>> {
    return this.http.post<ApiResponse<JournalEntry>>(`${this.baseUrl}/${id}/post`, {});
  }

  reverse(id: number): Observable<ApiResponse<JournalEntry>> {
    return this.http.post<ApiResponse<JournalEntry>>(`${this.baseUrl}/${id}/reverse`, {});
  }

  delete(id: number): Observable<ApiResponse<null>> {
    return this.http.delete<ApiResponse<null>>(`${this.baseUrl}/${id}`);
  }
}