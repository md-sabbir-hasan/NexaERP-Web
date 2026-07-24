import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { APP_CONFIG } from '../../../core/config/app.config';
import { ApiResponse } from '../../../core/models/api-response.model';
import { LedgerResponse } from '../models/ledger.model';
import { TrialBalanceResponse } from '../models/trial-balance.model';
import { PartyStatementResponse } from '../models/party-statement.model';
import { ProfitLossResponse } from '../models/profit-loss.model';
import { BalanceSheetResponse } from '../models/balance-sheet.model';
import { AgingResponse } from '../models/aging.model';

@Injectable({
  providedIn: 'root',
})
export class ReportService {
  private readonly baseUrl = `${APP_CONFIG.apiUrl}/reports`;

  constructor(private http: HttpClient) {}

  // Ledger Report
  getLedger(
    accountId: number,
    fromDate: string,
    toDate: string,
  ): Observable<ApiResponse<LedgerResponse>> {
    const params = new HttpParams().set('fromDate', fromDate).set('toDate', toDate);

    return this.http.get<ApiResponse<LedgerResponse>>(`${this.baseUrl}/ledger/${accountId}`, {
      params,
    });
  }

  getTrialBalance(asOfDate: string): Observable<ApiResponse<TrialBalanceResponse>> {
    const params = new HttpParams().set('asOfDate', asOfDate);

    return this.http.get<ApiResponse<TrialBalanceResponse>>(`${this.baseUrl}/trial-balance`, {
      params,
    });
  }

  getPartyStatement(
    partyId: number,
    fromDate: string,
    toDate: string,
  ): Observable<ApiResponse<PartyStatementResponse>> {
    const params = new HttpParams().set('fromDate', fromDate).set('toDate', toDate);

    return this.http.get<ApiResponse<PartyStatementResponse>>(
      `${this.baseUrl}/party-statement/${partyId}`,
      { params },
    );
  }

  // Download Party Statement as PDF
  downloadPartyStatementPdf(partyId: number, fromDate: string, toDate: string): Observable<Blob> {
    const params = new HttpParams()
      .set('partyId', partyId.toString())
      .set('fromDate', fromDate)
      .set('toDate', toDate);

    return this.http.get(`${APP_CONFIG.apiUrl}/reports/party-statement/pdf`, {
      params,
      responseType: 'blob' as const,
    });
  }

  //profit loss report
  getProfitLoss(fromDate: string, toDate: string): Observable<ApiResponse<ProfitLossResponse>> {
    const params = new HttpParams().set('fromDate', fromDate).set('toDate', toDate);

    return this.http.get<ApiResponse<ProfitLossResponse>>(`${this.baseUrl}/profit-loss`, {
      params,
    });
  }

  // get balance sheet report
  getBalanceSheet(asOfDate: string): Observable<ApiResponse<BalanceSheetResponse>> {
    const params = new HttpParams().set('asOfDate', asOfDate);

    return this.http.get<ApiResponse<BalanceSheetResponse>>(`${this.baseUrl}/balance-sheet`, {
      params,
    });
  }

  // get aging report
  getAgingReport(
    partyType: 'CUSTOMER' | 'VENDOR',
    asOfDate: string,
  ): Observable<ApiResponse<AgingResponse>> {
    const params = new HttpParams().set('partyType', partyType).set('asOfDate', asOfDate);

    return this.http.get<ApiResponse<AgingResponse>>(`${this.baseUrl}/aging`, {
      params,
    });
  }

  // ==== Excel exports ====

  downloadLedgerExcel(accountId: number, fromDate: string, toDate: string): Observable<Blob> {
    const params = new HttpParams()
      .set('accountId', accountId.toString())
      .set('fromDate', fromDate)
      .set('toDate', toDate);

    return this.http.get(`${this.baseUrl}/ledger/excel`, { params, responseType: 'blob' as const });
  }

  downloadTrialBalanceExcel(asOfDate: string): Observable<Blob> {
    const params = new HttpParams().set('asOfDate', asOfDate);

    return this.http.get(`${this.baseUrl}/trial-balance/excel`, {
      params,
      responseType: 'blob' as const,
    });
  }

  downloadProfitLossExcel(fromDate: string, toDate: string): Observable<Blob> {
    const params = new HttpParams().set('fromDate', fromDate).set('toDate', toDate);

    return this.http.get(`${this.baseUrl}/profit-loss/excel`, {
      params,
      responseType: 'blob' as const,
    });
  }

  downloadBalanceSheetExcel(asOfDate: string): Observable<Blob> {
    const params = new HttpParams().set('asOfDate', asOfDate);

    return this.http.get(`${this.baseUrl}/balance-sheet/excel`, {
      params,
      responseType: 'blob' as const,
    });
  }

  downloadPartyStatementExcel(partyId: number, fromDate: string, toDate: string): Observable<Blob> {
    const params = new HttpParams()
      .set('partyId', partyId.toString())
      .set('fromDate', fromDate)
      .set('toDate', toDate);

    return this.http.get(`${this.baseUrl}/party-statement/excel`, {
      params,
      responseType: 'blob' as const,
    });
  }

  downloadAgingExcel(partyType: 'CUSTOMER' | 'VENDOR', asOfDate: string): Observable<Blob> {
    const params = new HttpParams().set('partyType', partyType).set('asOfDate', asOfDate);

    return this.http.get(`${this.baseUrl}/aging/excel`, { params, responseType: 'blob' as const });
  }
}
