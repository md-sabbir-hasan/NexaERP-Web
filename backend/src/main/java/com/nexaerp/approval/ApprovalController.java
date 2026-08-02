package com.nexaerp.approval;

import com.nexaerp.approval.dto.*;
import com.nexaerp.common.response.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/approvals") @RequiredArgsConstructor
public class ApprovalController {
    private final ApprovalService service;
    @GetMapping("/pending") @PreAuthorize("hasAuthority('VIEW_APPROVAL_QUEUE')")
    public ResponseEntity<ApiResponse<PageResponseDto<ApprovalRequestResponseDto>>> pending(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return ResponseEntity.ok(ApiResponse.success(service.pending(page,bounded(size))));}
    @GetMapping("/pending/count") @PreAuthorize("hasAuthority('VIEW_APPROVAL_QUEUE')")
    public ResponseEntity<ApiResponse<Long>> count(){return ResponseEntity.ok(ApiResponse.success(service.pendingCount()));}
    @GetMapping("/my-requests") public ResponseEntity<ApiResponse<PageResponseDto<ApprovalRequestResponseDto>>> myRequests(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return ResponseEntity.ok(ApiResponse.success(service.myRequests(page,bounded(size))));}
    @GetMapping("/my-actions") public ResponseEntity<ApiResponse<PageResponseDto<ApprovalActionResponseDto>>> myActions(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return ResponseEntity.ok(ApiResponse.success(service.myActions(page,bounded(size))));}
    @GetMapping("/{id}") public ResponseEntity<ApiResponse<ApprovalRequestResponseDto>> get(@PathVariable Long id){return ResponseEntity.ok(ApiResponse.success(service.getById(id)));}
    @GetMapping("/entity/{entityType}/{entityId}/history") public ResponseEntity<ApiResponse<List<ApprovalRequestResponseDto>>> history(@PathVariable ApprovalEntityType entityType,@PathVariable Long entityId){return ResponseEntity.ok(ApiResponse.success(service.history(entityType,entityId)));}
    @PostMapping("/{id}/approve") @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ApprovalRequestResponseDto>> approve(@PathVariable Long id,@Valid @RequestBody(required=false) ApprovalDecisionDto dto){return ResponseEntity.ok(ApiResponse.success("Approval request approved",service.approve(id,dto)));}
    @PostMapping("/{id}/reject") @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ApprovalRequestResponseDto>> reject(@PathVariable Long id,@Valid @RequestBody ApprovalDecisionDto dto){return ResponseEntity.ok(ApiResponse.success("Approval request rejected",service.reject(id,dto)));}
    @PostMapping("/{id}/return") @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ApprovalRequestResponseDto>> returnForCorrection(@PathVariable Long id,@Valid @RequestBody ApprovalDecisionDto dto){return ResponseEntity.ok(ApiResponse.success("Approval request returned",service.returnForCorrection(id,dto)));}
    private int bounded(int size){return Math.max(1,Math.min(size,100));}
}
