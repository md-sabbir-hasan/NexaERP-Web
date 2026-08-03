package com.nexaerp.approval;

import com.nexaerp.account.Account;
import com.nexaerp.audit.AuditLogService;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.notification.*;
import com.nexaerp.party.*;
import com.nexaerp.security.*;
import com.nexaerp.user.*;
import com.nexaerp.vendorbill.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VendorBillApprovalWorkflowTest {
    @Mock ApprovalRequestRepository requests; @Mock ApprovalActionRepository actions;
    @Mock VendorBillRepository bills; @Mock UserRepository users; @Mock CurrentUserService currentUser;
    @Mock AuditLogService audit; @Mock NotificationService notifications;
    ApprovalProperties properties; ApprovalServiceImpl service;

    @BeforeEach void setUp() {
        properties = new ApprovalProperties(); properties.setEnabled(true); properties.getVendorBill().setEnabled(true);
        service = new ApprovalServiceImpl(properties, requests, actions,
                List.of(new VendorBillApprovalAdapter(properties, bills, audit)), users, currentUser, audit, notifications);
        lenient().when(requests.saveAndFlush(any())).thenAnswer(invocation -> { ApprovalRequest request = invocation.getArgument(0); request.setId(80L); return request; });
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void submitCreatesVendorBillRequestAndNotifiesApproversExceptMaker() {
        VendorBill bill = bill(10L, 1L); when(bills.findByIdForUpdate(10L)).thenReturn(Optional.of(bill)); when(currentUser.getCurrentUserId()).thenReturn(1L);
        when(requests.findByEntityTypeAndEntityIdAndActiveMarker(ApprovalEntityType.VENDOR_BILL, 10L, 1)).thenReturn(Optional.empty());
        when(requests.findTopByEntityTypeAndEntityIdOrderBySubmittedAtDesc(ApprovalEntityType.VENDOR_BILL, 10L)).thenReturn(Optional.empty());
        when(users.findById(1L)).thenReturn(Optional.of(user(1L, UserStatus.ACTIVE)));
        when(users.findDistinctByStatusAndPermissionCode(UserStatus.ACTIVE, "APPROVE_VENDOR_BILL")).thenReturn(List.of(user(1L, UserStatus.ACTIVE), user(2L, UserStatus.ACTIVE)));
        authenticate(1L, "CREATE_VENDOR_BILL");

        var result = service.submitVendorBill(10L);

        assertThat(result.getEntityType()).isEqualTo(ApprovalEntityType.VENDOR_BILL);
        assertThat(result.getRequiredPermission()).isEqualTo("APPROVE_VENDOR_BILL");
        assertThat(result.getEntityLabel()).isEqualTo("Vendor Bill");
        assertThat(result.getDocumentUrl()).isEqualTo("/vendor-bill/10");
        verify(notifications).scheduleUniqueForUsersAfterCommit(eq(List.of(2L)), eq(NotificationType.APPROVAL_SUBMITTED),
                eq(NotificationPriority.MEDIUM), eq(NotificationModule.APPROVAL), eq("Vendor bill approval requested"),
                eq("Vendor bill BILL-0010 is waiting for approval."), eq("/approvals/80"), eq("APPROVAL_REQUEST"), eq(80L));
    }

    @Test void approveSynchronizesBillAndRefreshesApprovedBaseline() {
        VendorBill bill = bill(10L, 1L); ApprovalRequest request = pending(); LocalDateTime approvedBaseline = bill.getUpdatedAt().plusSeconds(1);
        when(requests.findById(80L)).thenReturn(Optional.of(request)); when(bills.findByIdForUpdate(10L)).thenReturn(Optional.of(bill)); when(requests.findByIdForUpdate(80L)).thenReturn(Optional.of(request));
        when(currentUser.getCurrentUserId()).thenReturn(2L); when(users.findById(2L)).thenReturn(Optional.of(user(2L, UserStatus.ACTIVE)));
        when(bills.saveAndFlush(bill)).thenAnswer(invocation -> { bill.setUpdatedAt(approvedBaseline); return bill; }); authenticate(2L, "APPROVE_VENDOR_BILL");

        service.approve(80L, null);

        assertThat(bill.getStatus()).isEqualTo(VendorBillStatus.APPROVED);
        assertThat(bill.getApprovedBy()).isEqualTo(2L);
        assertThat(request.getDocumentUpdatedAt()).isEqualTo(approvedBaseline);
        assertThat(request.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
        verify(audit).log(com.nexaerp.audit.AuditAction.APPROVED, "VENDOR_BILL", 10L, "DRAFT", "APPROVED");
    }

    @Test void makerAndMissingPermissionCannotApprove() {
        VendorBill bill = bill(10L, 1L); ApprovalRequest request = pending();
        when(requests.findById(80L)).thenReturn(Optional.of(request)); when(bills.findByIdForUpdate(10L)).thenReturn(Optional.of(bill)); when(requests.findByIdForUpdate(80L)).thenReturn(Optional.of(request));
        when(currentUser.getCurrentUserId()).thenReturn(1L); when(users.findById(1L)).thenReturn(Optional.of(user(1L, UserStatus.ACTIVE))); authenticate(1L, "APPROVE_VENDOR_BILL");
        assertThatThrownBy(() -> service.approve(80L, null)).isInstanceOf(BusinessRuleException.class).hasMessageContaining("Maker");
        when(currentUser.getCurrentUserId()).thenReturn(2L); when(users.findById(2L)).thenReturn(Optional.of(user(2L, UserStatus.ACTIVE))); authenticate(2L);
        assertThatThrownBy(() -> service.approve(80L, null)).isInstanceOf(BusinessRuleException.class).hasMessageContaining("APPROVE_VENDOR_BILL");
    }

    private ApprovalRequest pending() { return ApprovalRequest.builder().id(80L).entityType(ApprovalEntityType.VENDOR_BILL).entityId(10L).documentNumber("BILL-0010").makerUserId(1L).status(ApprovalStatus.PENDING).requiredPermission("APPROVE_VENDOR_BILL").documentUpdatedAt(LocalDateTime.of(2026,8,2,10,0)).submittedAt(LocalDateTime.now()).activeMarker(1).build(); }
    private VendorBill bill(Long id, Long maker) { Party party = Party.builder().id(5L).name("Vendor").type(PartyType.VENDOR).isActive(true).build(); Account account = new Account(); account.setId(4L); account.setIsActive(true); VendorBillItem item = VendorBillItem.builder().expenseAccount(account).quantity(BigDecimal.ONE).unitPrice(BigDecimal.TEN).subTotal(BigDecimal.TEN).discountAmount(BigDecimal.ZERO).vatAmount(BigDecimal.ZERO).tdsAmount(BigDecimal.ZERO).build(); VendorBill bill = VendorBill.builder().id(id).billNumber("BILL-0010").party(party).status(VendorBillStatus.DRAFT).subTotal(BigDecimal.TEN).discountAmount(BigDecimal.ZERO).vatAmount(BigDecimal.ZERO).tdsAmount(BigDecimal.ZERO).grandTotal(BigDecimal.TEN).netPayable(BigDecimal.TEN).dueAmount(BigDecimal.TEN).items(List.of(item)).build(); bill.setCreatedBy(maker); bill.setUpdatedAt(LocalDateTime.of(2026,8,2,10,0)); return bill; }
    private User user(Long id, UserStatus status) { return User.builder().id(id).name("User " + id).email("u" + id + "@test.local").status(status).build(); }
    private void authenticate(Long id, String... permissions) { var authorities = Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList(); SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new CurrentUserPrincipal(id,"u@test.local"),"n/a",authorities)); }
}
