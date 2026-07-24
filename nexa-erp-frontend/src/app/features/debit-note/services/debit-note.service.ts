import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiResponse } from '../../../core/models/api-response.model';
import { DebitNote, DebitNoteCancelledReason, DebitNoteRequest } from '../models/debit-note.model';

@Injectable({ providedIn: 'root' })
export class DebitNoteService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8085/api/debit-notes';

  getAll(): Observable<ApiResponse<DebitNote[]>> {
    return this.http.get<ApiResponse<DebitNote[]>>(this.apiUrl);
  }
  getById(id: number): Observable<ApiResponse<DebitNote>> {
    return this.http.get<ApiResponse<DebitNote>>(`${this.apiUrl}/${id}`);
  }
  create(body: DebitNoteRequest): Observable<ApiResponse<DebitNote>> {
    return this.http.post<ApiResponse<DebitNote>>(this.apiUrl, body);
  }
  update(id: number, body: DebitNoteRequest): Observable<ApiResponse<DebitNote>> {
    return this.http.put<ApiResponse<DebitNote>>(`${this.apiUrl}/${id}`, body);
  }
  approve(id: number): Observable<ApiResponse<DebitNote>> {
    return this.http.patch<ApiResponse<DebitNote>>(`${this.apiUrl}/${id}/approve`, {});
  }
  post(id: number): Observable<ApiResponse<DebitNote>> {
    return this.http.patch<ApiResponse<DebitNote>>(`${this.apiUrl}/${id}/post`, {});
  }
  cancel(id: number, reason: DebitNoteCancelledReason): Observable<ApiResponse<DebitNote>> {
    return this.http.patch<ApiResponse<DebitNote>>(
      `${this.apiUrl}/${id}/cancel`,
      {},
      { params: new HttpParams().set('reason', reason) },
    );
  }
  delete(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/${id}`);
  }
}
