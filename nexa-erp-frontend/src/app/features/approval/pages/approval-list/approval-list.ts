import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApprovalAction, ApprovalRequest, PageResponse } from '../../models/approval.model';
import { ApiResponse } from '../../../../core/models/api-response.model';
import { ApprovalService } from '../../services/approval.service';

@Component({selector:'app-approval-list',standalone:true,imports:[CommonModule,RouterLink],templateUrl:'./approval-list.html',styleUrl:'./approval-list.scss'})
export class ApprovalList implements OnInit {
  readonly requests=signal<ApprovalRequest[]>([]); readonly actions=signal<ApprovalAction[]>([]);
  readonly loading=signal(false); readonly error=signal<string|null>(null); readonly page=signal(0); readonly totalPages=signal(0);
  mode:'pending'|'requests'|'actions'='pending';
  constructor(private readonly service:ApprovalService,private readonly route:ActivatedRoute){}
  ngOnInit(){this.mode=this.route.snapshot.data['mode']??'pending';this.load();}
  load(page=this.page()){
    this.loading.set(true);this.error.set(null);
    if(this.mode==='actions'){
      this.service.myActions(page).subscribe({next:r=>this.acceptActions(r),error:e=>this.fail(e)});
    }else{
      const call=this.mode==='pending'?this.service.pending(page):this.service.myRequests(page);
      call.subscribe({next:r=>this.acceptRequests(r),error:e=>this.fail(e)});
    }
  }
  private acceptRequests(res:ApiResponse<PageResponse<ApprovalRequest>>){this.requests.set(res.data.content);this.finish(res.data);}
  private acceptActions(res:ApiResponse<PageResponse<ApprovalAction>>){this.actions.set(res.data.content);this.finish(res.data);}
  private finish(page:PageResponse<unknown>){this.page.set(page.page);this.totalPages.set(page.totalPages);this.loading.set(false);}
  private fail(error:unknown){const response=error as {error?:{message?:string}};this.error.set(response.error?.message??'Could not load approvals');this.loading.set(false);}
  title(){return this.mode==='pending'?'Approval Queue':this.mode==='requests'?'My Requests':'My Actions';}
}
