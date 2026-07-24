import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiResponse } from '../../../core/models/api-response.model';
import { VendorBillOption } from '../models/vendor-bill-option.model';

@Injectable({ providedIn: 'root' })
export class DebitNoteVendorBillService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8085/api/vendor-bills';

  getAll(): Observable<ApiResponse<VendorBillOption[]>> {
    return this.http.get<ApiResponse<VendorBillOption[]>>(this.apiUrl);
  }

  getById(id: number): Observable<ApiResponse<VendorBillOption>> {
    return this.http.get<ApiResponse<VendorBillOption>>(`${this.apiUrl}/${id}`);
  }
}
