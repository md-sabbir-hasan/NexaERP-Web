import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AlertService } from '../../../../core/services/alert.service';
import { ApprovalRequest } from '../../models/approval.model';
import { ApprovalService } from '../../services/approval.service';

@Component({selector:'app-approval-detail',standalone:true,imports:[CommonModule,FormsModule,RouterLink],templateUrl:'./approval-detail.html',styleUrl:'../approval-list/approval-list.scss'})
export class ApprovalDetail implements OnInit {
  readonly request=signal<ApprovalRequest|null>(null);readonly loading=signal(false);readonly error=signal<string|null>(null);readonly deciding=signal(false);
  decision:'approve'|'reject'|'return'|null=null;comment='';private id=0;
  constructor(private readonly service:ApprovalService,private readonly route:ActivatedRoute,private readonly alert:AlertService){}
  ngOnInit(){this.id=Number(this.route.snapshot.paramMap.get('id'));this.load();}
  load(){this.loading.set(true);this.service.get(this.id).subscribe({next:r=>{this.request.set(r.data);this.loading.set(false);},error:e=>{this.error.set(e?.error?.message??'Could not load approval');this.loading.set(false);}});}
  open(action:'approve'|'reject'|'return'){this.decision=action;this.comment='';}
  close(){if(!this.deciding())this.decision=null;}
  submit(){if(!this.decision)return;if(this.decision!=='approve'&&!this.comment.trim()){this.alert.error('A comment is required');return;}this.deciding.set(true);this.service.decide(this.id,this.decision,this.comment.trim()||null).subscribe({next:r=>{this.request.set(r.data);this.decision=null;this.deciding.set(false);this.alert.success('Approval decision saved');},error:e=>{this.deciding.set(false);this.alert.error(e?.error?.message??'Decision failed');}});}
  entityLabel(request:ApprovalRequest){return request.entityLabel;}
  documentLink(request:ApprovalRequest){return request.documentUrl;}
}
