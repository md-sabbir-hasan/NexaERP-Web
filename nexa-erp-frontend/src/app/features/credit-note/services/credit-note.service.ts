import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiResponse } from '../../../core/models/api-response.model';
import {
  CreditNote,
  CreditNoteCancelledReason,
  CreditNoteRequest,
} from '../models/credit-note.model';

@Injectable({ providedIn: 'root' })
export class CreditNoteService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8085/api/credit-notes';

  getAll(): Observable<ApiResponse<CreditNote[]>> {
    return this.http.get<ApiResponse<CreditNote[]>>(this.apiUrl);
  }
  getById(id: number): Observable<ApiResponse<CreditNote>> {
    return this.http.get<ApiResponse<CreditNote>>(`${this.apiUrl}/${id}`);
  }
  create(body: CreditNoteRequest): Observable<ApiResponse<CreditNote>> {
    return this.http.post<ApiResponse<CreditNote>>(this.apiUrl, body);
  }
  update(id: number, body: CreditNoteRequest): Observable<ApiResponse<CreditNote>> {
    return this.http.put<ApiResponse<CreditNote>>(`${this.apiUrl}/${id}`, body);
  }
  approve(id: number): Observable<ApiResponse<CreditNote>> {
    return this.http.patch<ApiResponse<CreditNote>>(`${this.apiUrl}/${id}/approve`, {});
  }
  post(id: number): Observable<ApiResponse<CreditNote>> {
    return this.http.patch<ApiResponse<CreditNote>>(`${this.apiUrl}/${id}/post`, {});
  }
  cancel(id: number, reason: CreditNoteCancelledReason): Observable<ApiResponse<CreditNote>> {
    return this.http.patch<ApiResponse<CreditNote>>(
      `${this.apiUrl}/${id}/cancel`,
      {},
      { params: new HttpParams().set('reason', reason) },
    );
  }
  delete(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/${id}`);
  }
}
