import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { APP_CONFIG } from '../../../core/config/app.config';
import { ApiResponse } from '../../../core/models/api-response.model';
import { Party, PartyType } from '../models/party.model';

export interface PartyRequest {
  name: string;
  type: PartyType;
  notes?: string | null;

  companyName?: string | null;
  contactPerson?: string | null;
  jobPosition?: string | null;

  email?: string | null;
  phone: string;
  mobile?: string | null;

  street?: string | null;
  city?: string | null;
  state?: string | null;
  country?: string | null;

  creditLimit?: number;
  paymentTerms?: number;
  openingBalance?: number;
  currency?: string;
  bankAccountNo?: string | null;
  bankName?: string | null;

  bin?: string | null;
  tin?: string | null;
  vatRegistered?: boolean;

  tradeLicenseNo?: string | null;
  tradeLicenseExpiry?: string | null;
  binCertificateNo?: string | null;
  tinCertificateNo?: string | null;
  nidNo?: string | null;
}

export interface FileUploadResponse {
  fileName: string;
  originalName: string;
  fileUrl: string;
  fileType: string;
  fileSize: number;
  entityType: string;
  entityId: number;
}

@Injectable({
  providedIn: 'root',
})
export class PartyService {
  private readonly baseUrl = `${APP_CONFIG.apiUrl}/parties`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<ApiResponse<Party[]>> {
    return this.http.get<ApiResponse<Party[]>>(this.baseUrl);
  }

  getById(id: number): Observable<ApiResponse<Party>> {
    return this.http.get<ApiResponse<Party>>(`${this.baseUrl}/${id}`);
  }

  getByType(type: PartyType): Observable<ApiResponse<Party[]>> {
    return this.http.get<ApiResponse<Party[]>>(`${this.baseUrl}/type/${type}`);
  }

  create(request: PartyRequest): Observable<ApiResponse<Party>> {
    return this.http.post<ApiResponse<Party>>(this.baseUrl, request);
  }

  update(id: number, request: PartyRequest): Observable<ApiResponse<Party>> {
    return this.http.put<ApiResponse<Party>>(`${this.baseUrl}/${id}`, request);
  }

  deactivate(id: number): Observable<ApiResponse<void>> {
    return this.http.patch<ApiResponse<void>>(`${this.baseUrl}/${id}/deactivate`, {});
  }

  activate(id: number): Observable<ApiResponse<void>> {
    return this.http.patch<ApiResponse<void>>(`${this.baseUrl}/${id}/activate`, {});
  }

  uploadTradeLicense(id: number, file: File): Observable<ApiResponse<FileUploadResponse>> {
    return this.uploadDocument(id, file, 'trade-license');
  }

  uploadBinCertificate(id: number, file: File): Observable<ApiResponse<FileUploadResponse>> {
    return this.uploadDocument(id, file, 'bin-certificate');
  }

  uploadTinCertificate(id: number, file: File): Observable<ApiResponse<FileUploadResponse>> {
    return this.uploadDocument(id, file, 'tin-certificate');
  }

  uploadNid(id: number, file: File): Observable<ApiResponse<FileUploadResponse>> {
    return this.uploadDocument(id, file, 'nid');
  }

  private uploadDocument(
    id: number,
    file: File,
    documentType: string,
  ): Observable<ApiResponse<FileUploadResponse>> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post<ApiResponse<FileUploadResponse>>(
      `${this.baseUrl}/${id}/documents/${documentType}`,
      formData,
    );
  }
}