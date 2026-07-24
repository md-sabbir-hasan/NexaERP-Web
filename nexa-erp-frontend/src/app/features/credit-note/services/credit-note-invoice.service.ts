import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiResponse } from '../../../core/models/api-response.model';
import { InvoiceOption } from '../models/invoice-option.model';

@Injectable({
  providedIn: 'root',
})
export class CreditNoteInvoiceService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl = 'http://localhost:8085/api/invoices';

  getAll(): Observable<ApiResponse<InvoiceOption[]>> {
    return this.http.get<ApiResponse<InvoiceOption[]>>(this.apiUrl);
  }

  getById(id: number): Observable<ApiResponse<InvoiceOption>> {
    return this.http.get<ApiResponse<InvoiceOption>>(`${this.apiUrl}/${id}`);
  }
}
