import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { APP_CONFIG } from '../../../core/config/app.config';
import { ApiResponse } from '../../../core/models/api-response.model';
import { BankTransaction, BankTransactionRequest } from '../models/bank-transaction.model';
import {
  BankReconciliation,
  BankReconciliationStartRequest,
  BankStatementLine,
  StatementImportResult,
} from '../models/bank-reconciliation.model';

@Injectable({
  providedIn: 'root',
})
export class BankReconciliationService {
  private readonly baseUrl = `${APP_CONFIG.apiUrl}/bank-reconciliations`;

  constructor(private http: HttpClient) {}

  start(request: BankReconciliationStartRequest): Observable<ApiResponse<BankReconciliation>> {
    return this.http.post<ApiResponse<BankReconciliation>>(`${this.baseUrl}/start`, request);
  }

  getById(id: number): Observable<ApiResponse<BankReconciliation>> {
    return this.http.get<ApiResponse<BankReconciliation>>(`${this.baseUrl}/${id}`);
  }

  getByAccount(bankAccountId: number): Observable<ApiResponse<BankReconciliation[]>> {
    return this.http.get<ApiResponse<BankReconciliation[]>>(
      `${this.baseUrl}/account/${bankAccountId}`,
    );
  }

  getUnmatchedTransactions(id: number): Observable<ApiResponse<BankTransaction[]>> {
    return this.http.get<ApiResponse<BankTransaction[]>>(`${this.baseUrl}/${id}/unmatched-transactions`);
  }

  match(id: number, transactionIds: number[]): Observable<ApiResponse<BankReconciliation>> {
    return this.http.patch<ApiResponse<BankReconciliation>>(`${this.baseUrl}/${id}/match`, {
      transactionIds,
    });
  }

  unmatch(id: number, transactionId: number): Observable<ApiResponse<BankReconciliation>> {
    return this.http.patch<ApiResponse<BankReconciliation>>(
      `${this.baseUrl}/${id}/unmatch/${transactionId}`,
      {},
    );
  }

  addAdjustment(
    id: number,
    request: BankTransactionRequest,
  ): Observable<ApiResponse<BankReconciliation>> {
    return this.http.post<ApiResponse<BankReconciliation>>(`${this.baseUrl}/${id}/adjustment`, request);
  }

  complete(id: number): Observable<ApiResponse<BankReconciliation>> {
    return this.http.post<ApiResponse<BankReconciliation>>(`${this.baseUrl}/${id}/complete`, {});
  }

  reopen(id: number): Observable<ApiResponse<BankReconciliation>> {
    return this.http.post<ApiResponse<BankReconciliation>>(`${this.baseUrl}/${id}/reopen`, {});
  }

  importStatement(id: number, file: File): Observable<ApiResponse<StatementImportResult>> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ApiResponse<StatementImportResult>>(
      `${this.baseUrl}/${id}/import-statement`,
      formData,
    );
  }

  getStatementLines(id: number): Observable<ApiResponse<BankStatementLine[]>> {
    return this.http.get<ApiResponse<BankStatementLine[]>>(`${this.baseUrl}/${id}/statement-lines`);
  }

  matchStatementLine(
    id: number,
    lineId: number,
    transactionId: number,
  ): Observable<ApiResponse<BankStatementLine>> {
    return this.http.patch<ApiResponse<BankStatementLine>>(
      `${this.baseUrl}/${id}/statement-lines/${lineId}/match`,
      { transactionId },
    );
  }

  convertLineToAdjustment(
    id: number,
    lineId: number,
    contraAccountId: number,
    description: string | null,
  ): Observable<ApiResponse<BankStatementLine>> {
    return this.http.post<ApiResponse<BankStatementLine>>(
      `${this.baseUrl}/${id}/statement-lines/${lineId}/convert-to-adjustment`,
      { contraAccountId, description },
    );
  }
}
