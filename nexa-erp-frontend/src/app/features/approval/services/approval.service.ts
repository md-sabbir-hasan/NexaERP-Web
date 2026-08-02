import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { APP_CONFIG } from '../../../core/config/app.config';
import { ApiResponse } from '../../../core/models/api-response.model';
import { ApprovalAction, ApprovalEntityType, ApprovalRequest, PageResponse } from '../models/approval.model';

@Injectable({providedIn:'root'})
export class ApprovalService {
  private readonly url=`${APP_CONFIG.apiUrl}/approvals`;
  constructor(private readonly http:HttpClient){}
  pending(page=0,size=20):Observable<ApiResponse<PageResponse<ApprovalRequest>>>{return this.http.get<ApiResponse<PageResponse<ApprovalRequest>>>(`${this.url}/pending`,{params:{page,size}});}
  myRequests(page=0,size=20):Observable<ApiResponse<PageResponse<ApprovalRequest>>>{return this.http.get<ApiResponse<PageResponse<ApprovalRequest>>>(`${this.url}/my-requests`,{params:{page,size}});}
  myActions(page=0,size=20):Observable<ApiResponse<PageResponse<ApprovalAction>>>{return this.http.get<ApiResponse<PageResponse<ApprovalAction>>>(`${this.url}/my-actions`,{params:{page,size}});}
  get(id:number):Observable<ApiResponse<ApprovalRequest>>{return this.http.get<ApiResponse<ApprovalRequest>>(`${this.url}/${id}`);}
  history(entityType:ApprovalEntityType,entityId:number):Observable<ApiResponse<ApprovalRequest[]>>{return this.http.get<ApiResponse<ApprovalRequest[]>>(`${this.url}/entity/${entityType}/${entityId}/history`);}
  decide(id:number,action:'approve'|'reject'|'return',comment:string|null):Observable<ApiResponse<ApprovalRequest>>{return this.http.post<ApiResponse<ApprovalRequest>>(`${this.url}/${id}/${action}`,{comment});}
}
