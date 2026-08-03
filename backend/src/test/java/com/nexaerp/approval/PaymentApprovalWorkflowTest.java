package com.nexaerp.approval;

import com.nexaerp.account.Account;
import com.nexaerp.approval.dto.ApprovalDecisionDto;
import com.nexaerp.audit.AuditLogService;
import com.nexaerp.banking.entity.BankAccount;
import com.nexaerp.banking.repository.BankAccountRepository;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.expense.ExpenseRepository;
import com.nexaerp.invoice.InvoiceRepository;
import com.nexaerp.notification.*;
import com.nexaerp.party.*;
import com.nexaerp.payment.*;
import com.nexaerp.security.*;
import com.nexaerp.user.*;
import com.nexaerp.vendorbill.VendorBillRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentApprovalWorkflowTest {
    @Mock ApprovalRequestRepository requests;
    @Mock ApprovalActionRepository actions;
    @Mock PaymentRepository payments;
    @Mock PaymentAllocationRepository allocations;
    @Mock BankAccountRepository banks;
    @Mock InvoiceRepository invoices;
    @Mock VendorBillRepository bills;
    @Mock ExpenseRepository expenses;
    @Mock UserRepository users;
    @Mock CurrentUserService currentUser;
    @Mock AuditLogService audit;
    @Mock NotificationService notifications;
    ApprovalProperties properties;
    ApprovalServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new ApprovalProperties();
        properties.setEnabled(true);
        properties.getPayment().setEnabled(true);
        var adapter = new PaymentApprovalAdapter(properties, payments, allocations, banks, invoices, bills, expenses);
        service = new ApprovalServiceImpl(properties, requests, actions, List.of(adapter), users,
                currentUser, audit, notifications);
        lenient().when(requests.saveAndFlush(any())).thenAnswer(invocation -> {
            ApprovalRequest request = invocation.getArgument(0);
            request.setId(100L);
            return request;
        });
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void submitCreatesGenericPaymentRequestAndNotification() {
        Payment payment = payment();
        stubValidDocument(payment);
        when(currentUser.getCurrentUserId()).thenReturn(1L);
        when(requests.findByEntityTypeAndEntityIdAndActiveMarker(ApprovalEntityType.PAYMENT, 10L, 1))
                .thenReturn(Optional.empty());
        when(requests.findTopByEntityTypeAndEntityIdOrderBySubmittedAtDesc(ApprovalEntityType.PAYMENT, 10L))
                .thenReturn(Optional.empty());
        when(users.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(users.findDistinctByStatusAndPermissionCode(UserStatus.ACTIVE, "APPROVE_PAYMENT"))
                .thenReturn(List.of(user(1L), user(2L)));
        authenticate(1L, "CREATE_PAYMENT");

        var result = service.submitPayment(10L);

        assertThat(result.getEntityType()).isEqualTo(ApprovalEntityType.PAYMENT);
        assertThat(result.getRequiredPermission()).isEqualTo("APPROVE_PAYMENT");
        assertThat(result.getDocumentUrl()).isEqualTo("/payment/10");
        verify(notifications).scheduleUniqueForUsersAfterCommit(eq(List.of(2L)),
                eq(NotificationType.APPROVAL_SUBMITTED), eq(NotificationPriority.MEDIUM),
                eq(NotificationModule.APPROVAL), eq("Payment approval requested"),
                eq("Payment PAY-2026-000010 is waiting for approval."), eq("/approvals/100"),
                eq("APPROVAL_REQUEST"), eq(100L));
    }

    @Test
    void approveLeavesPaymentDraftAndDoesNotCreatePaymentAudit() {
        Payment payment = payment();
        ApprovalRequest request = pending(payment.getUpdatedAt());
        when(requests.findById(100L)).thenReturn(Optional.of(request));
        when(payments.findByIdForUpdate(10L)).thenReturn(Optional.of(payment));
        when(requests.findByIdForUpdate(100L)).thenReturn(Optional.of(request));
        when(currentUser.getCurrentUserId()).thenReturn(2L);
        when(users.findById(2L)).thenReturn(Optional.of(user(2L)));
        authenticate(2L, "APPROVE_PAYMENT");

        service.approve(100L, null);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DRAFT);
        assertThat(request.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(request.getDocumentUpdatedAt()).isEqualTo(payment.getUpdatedAt());
        verify(payments, never()).save(any());
        verify(audit, never()).log(any(), eq("PAYMENT"), any(), any(), any());
    }

    @Test
    void rejectAndReturnRequireCommentsAndLeavePaymentDraft() {
        Payment payment = payment();
        assertThatThrownBy(() -> service.reject(100L, new ApprovalDecisionDto()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("comment");
        assertThatThrownBy(() -> service.returnForCorrection(100L, new ApprovalDecisionDto()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("comment");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DRAFT);
    }

    @Test
    void rejectAndReturnChangeOnlyTheGenericApprovalRequest() {
        Payment payment = payment();
        ApprovalRequest rejected = pending(payment.getUpdatedAt());
        stubDecision(payment, rejected);

        service.reject(100L, decision("Incorrect allocation"));

        assertThat(rejected.getStatus()).isEqualTo(ApprovalStatus.REJECTED);
        assertThat(rejected.getActiveMarker()).isNull();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DRAFT);

        ApprovalRequest returned = pending(payment.getUpdatedAt());
        stubDecision(payment, returned);
        service.returnForCorrection(100L, decision("Update reference"));

        assertThat(returned.getStatus()).isEqualTo(ApprovalStatus.RETURNED);
        assertThat(returned.getActiveMarker()).isNull();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DRAFT);
    }

    @Test
    void invalidBankAmountAndAllocationTotalsBlockSubmission() {
        Payment payment = payment();
        stubValidDocument(payment);
        payment.setAmount(BigDecimal.ZERO);
        assertThatThrownBy(() -> service.submitPayment(10L)).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("greater than zero");

        payment.setAmount(new BigDecimal("100.00"));
        payment.setAllocatedAmount(BigDecimal.TEN);
        assertThatThrownBy(() -> service.submitPayment(10L)).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("allocation totals");
    }

    private void stubValidDocument(Payment payment) {
        when(payments.findByIdForUpdate(10L)).thenReturn(Optional.of(payment));
        lenient().when(allocations.findByPaymentId(10L)).thenReturn(List.of());
        lenient().when(banks.findByCoaAccountId(20L)).thenReturn(Optional.of(BankAccount.builder()
                .id(30L).coaAccountId(20L).currency("BDT").isActive(true).currentBalance(BigDecimal.TEN).build()));
    }

    private Payment payment() {
        Party party = Party.builder().id(5L).name("Vendor").type(PartyType.VENDOR).isActive(true).build();
        Account account = new Account();
        account.setId(20L);
        account.setCode("1010");
        account.setIsActive(true);
        account.setCurrentBalance(BigDecimal.TEN);
        Payment payment = Payment.builder().id(10L).paymentNumber("PAY-2026-000010")
                .paymentDate(LocalDate.of(2026, 8, 3)).paymentType(PaymentType.PAYMENT)
                .party(party).account(account).amount(new BigDecimal("100.00"))
                .allocatedAmount(BigDecimal.ZERO).unallocatedAmount(new BigDecimal("100.00"))
                .currencyCode("BDT").exchangeRate(BigDecimal.ONE).paymentMethod(PaymentMethod.CASH)
                .status(PaymentStatus.DRAFT).build();
        payment.setCreatedBy(1L);
        payment.setUpdatedAt(LocalDateTime.of(2026, 8, 3, 10, 0));
        return payment;
    }

    private ApprovalRequest pending(LocalDateTime snapshot) {
        return ApprovalRequest.builder().id(100L).entityType(ApprovalEntityType.PAYMENT).entityId(10L)
                .documentNumber("PAY-2026-000010").makerUserId(1L).status(ApprovalStatus.PENDING)
                .requiredPermission("APPROVE_PAYMENT").documentUpdatedAt(snapshot)
                .submittedAt(LocalDateTime.now()).activeMarker(1).build();
    }

    private void stubDecision(Payment payment, ApprovalRequest request) {
        when(requests.findById(100L)).thenReturn(Optional.of(request));
        when(payments.findByIdForUpdate(10L)).thenReturn(Optional.of(payment));
        when(requests.findByIdForUpdate(100L)).thenReturn(Optional.of(request));
        when(currentUser.getCurrentUserId()).thenReturn(2L);
        when(users.findById(2L)).thenReturn(Optional.of(user(2L)));
        authenticate(2L, "APPROVE_PAYMENT");
    }

    private ApprovalDecisionDto decision(String comment) {
        ApprovalDecisionDto dto = new ApprovalDecisionDto();
        dto.setComment(comment);
        return dto;
    }

    private User user(Long id) {
        return User.builder().id(id).name("User " + id).email("u" + id + "@test.local")
                .status(UserStatus.ACTIVE).build();
    }

    private void authenticate(Long id, String... permissions) {
        var authorities = Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new CurrentUserPrincipal(id, "u@test.local"), "n/a", authorities));
    }
}
