package com.nexaerp.approval;

import com.nexaerp.audit.AuditLogService;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.invoice.*;
import com.nexaerp.notification.*;
import com.nexaerp.party.*;
import com.nexaerp.security.*;
import com.nexaerp.user.*;
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
class InvoiceApprovalWorkflowTest {
    @Mock ApprovalRequestRepository requests;
    @Mock ApprovalActionRepository actions;
    @Mock InvoiceRepository invoices;
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
        properties.getInvoice().setEnabled(true);
        service = new ApprovalServiceImpl(properties, requests, actions,
                List.of(new InvoiceApprovalAdapter(properties, invoices)), users, currentUser, audit, notifications);
        lenient().when(requests.saveAndFlush(any())).thenAnswer(invocation -> {
            ApprovalRequest request = invocation.getArgument(0);
            request.setId(90L);
            return request;
        });
    }

    @AfterEach void clearSecurity() { SecurityContextHolder.clearContext(); }

    @Test
    void submitCreatesInvoiceRequestAndNotifiesApproversExceptMaker() {
        Invoice invoice = invoice(InvoiceStatus.DRAFT, 1L);
        when(invoices.findByIdForUpdate(10L)).thenReturn(Optional.of(invoice));
        when(currentUser.getCurrentUserId()).thenReturn(1L);
        when(requests.findByEntityTypeAndEntityIdAndActiveMarker(ApprovalEntityType.INVOICE, 10L, 1)).thenReturn(Optional.empty());
        when(requests.findTopByEntityTypeAndEntityIdOrderBySubmittedAtDesc(ApprovalEntityType.INVOICE, 10L)).thenReturn(Optional.empty());
        when(users.findById(1L)).thenReturn(Optional.of(user(1L, UserStatus.ACTIVE)));
        when(users.findDistinctByStatusAndPermissionCode(UserStatus.ACTIVE, "APPROVE_INVOICE"))
                .thenReturn(List.of(user(1L, UserStatus.ACTIVE), user(2L, UserStatus.ACTIVE)));
        authenticate(1L, "CREATE_INVOICE");

        var result = service.submitInvoice(10L);

        assertThat(result.getEntityType()).isEqualTo(ApprovalEntityType.INVOICE);
        assertThat(result.getRequiredPermission()).isEqualTo("APPROVE_INVOICE");
        assertThat(result.getEntityLabel()).isEqualTo("Invoice");
        assertThat(result.getDocumentUrl()).isEqualTo("/invoice/10");
        verify(notifications).scheduleUniqueForUsersAfterCommit(eq(List.of(2L)), eq(NotificationType.APPROVAL_SUBMITTED),
                eq(NotificationPriority.MEDIUM), eq(NotificationModule.APPROVAL), eq("Invoice approval requested"),
                eq("Invoice INV-0010 is waiting for approval."), eq("/approvals/90"), eq("APPROVAL_REQUEST"), eq(90L));
    }

    @Test
    void approveLeavesInvoiceDraftAndSnapshotUnchanged() {
        Invoice invoice = invoice(InvoiceStatus.DRAFT, 1L);
        ApprovalRequest request = pending(invoice.getUpdatedAt());
        when(requests.findById(90L)).thenReturn(Optional.of(request));
        when(invoices.findByIdForUpdate(10L)).thenReturn(Optional.of(invoice));
        when(requests.findByIdForUpdate(90L)).thenReturn(Optional.of(request));
        when(currentUser.getCurrentUserId()).thenReturn(2L);
        when(users.findById(2L)).thenReturn(Optional.of(user(2L, UserStatus.ACTIVE)));
        authenticate(2L, "APPROVE_INVOICE");

        service.approve(90L, null);

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(request.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(request.getDocumentUpdatedAt()).isEqualTo(invoice.getUpdatedAt());
        verify(invoices, never()).save(any());
        verify(audit, never()).log(any(), eq("INVOICE"), any(), any(), any());
    }

    @Test
    void nonOwnerAndInvalidCustomerCannotSubmit() {
        Invoice invoice = invoice(InvoiceStatus.DRAFT, 1L);
        when(invoices.findByIdForUpdate(10L)).thenReturn(Optional.of(invoice));
        when(currentUser.getCurrentUserId()).thenReturn(2L);
        assertThatThrownBy(() -> service.submitInvoice(10L)).isInstanceOf(BusinessRuleException.class).hasMessageContaining("creator");

        invoice.getParty().setIsActive(false);
        assertThatThrownBy(() -> service.submitInvoice(10L)).isInstanceOf(BusinessRuleException.class).hasMessageContaining("active customer");
    }

    @Test
    void totalsMismatchAndNonDraftCannotSubmit() {
        Invoice invoice = invoice(InvoiceStatus.DRAFT, 1L);
        when(invoices.findByIdForUpdate(10L)).thenReturn(Optional.of(invoice));
        invoice.setGrandTotal(new BigDecimal("99.00"));
        assertThatThrownBy(() -> service.submitInvoice(10L)).isInstanceOf(BusinessRuleException.class).hasMessageContaining("totals");

        invoice.setGrandTotal(new BigDecimal("100.00"));
        invoice.setStatus(InvoiceStatus.POSTED);
        assertThatThrownBy(() -> service.submitInvoice(10L)).isInstanceOf(BusinessRuleException.class).hasMessageContaining("DRAFT");
    }

    @Test
    void rejectedRequestRequiresInvoiceEditBeforeResubmission() {
        Invoice invoice = invoice(InvoiceStatus.DRAFT, 1L);
        ApprovalRequest previous = pending(invoice.getUpdatedAt());
        previous.setStatus(ApprovalStatus.REJECTED);
        previous.setActiveMarker(null);
        when(invoices.findByIdForUpdate(10L)).thenReturn(Optional.of(invoice));
        when(currentUser.getCurrentUserId()).thenReturn(1L);
        when(requests.findByEntityTypeAndEntityIdAndActiveMarker(ApprovalEntityType.INVOICE, 10L, 1)).thenReturn(Optional.empty());
        when(requests.findTopByEntityTypeAndEntityIdOrderBySubmittedAtDesc(ApprovalEntityType.INVOICE, 10L)).thenReturn(Optional.of(previous));

        assertThatThrownBy(() -> service.submitInvoice(10L)).isInstanceOf(BusinessRuleException.class).hasMessageContaining("corrected");
    }

    private ApprovalRequest pending(LocalDateTime snapshot) {
        return ApprovalRequest.builder().id(90L).entityType(ApprovalEntityType.INVOICE).entityId(10L)
                .documentNumber("INV-0010").makerUserId(1L).status(ApprovalStatus.PENDING)
                .requiredPermission("APPROVE_INVOICE").documentUpdatedAt(snapshot)
                .submittedAt(LocalDateTime.now()).activeMarker(1).build();
    }

    private Invoice invoice(InvoiceStatus status, Long maker) {
        Invoice invoice = Invoice.builder().id(10L).invoiceNumber("INV-0010")
                .invoiceDate(LocalDate.of(2026, 8, 1)).dueDate(LocalDate.of(2026, 8, 31))
                .paymentTerms(30).party(Party.builder().id(5L).name("Customer").type(PartyType.CUSTOMER).isActive(true).build())
                .status(status).subTotal(new BigDecimal("100.00")).discountAmount(BigDecimal.ZERO)
                .vatAmount(BigDecimal.ZERO).grandTotal(new BigDecimal("100.00")).paidAmount(BigDecimal.ZERO)
                .dueAmount(new BigDecimal("100.00")).build();
        InvoiceItem item = InvoiceItem.builder().id(11L).invoice(invoice).description("Service")
                .quantity(BigDecimal.ONE).unitPrice(new BigDecimal("100.00")).discountPercent(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO).vatRate(BigDecimal.ZERO).vatAmount(BigDecimal.ZERO)
                .subTotal(new BigDecimal("100.00")).lineTotal(new BigDecimal("100.00")).build();
        invoice.setItems(new ArrayList<>(List.of(item)));
        invoice.setCreatedBy(maker);
        invoice.setUpdatedAt(LocalDateTime.of(2026, 8, 2, 10, 0));
        return invoice;
    }

    private User user(Long id, UserStatus status) {
        return User.builder().id(id).name("User " + id).email("u" + id + "@test.local").status(status).build();
    }

    private void authenticate(Long id, String... permissions) {
        var authorities = Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new CurrentUserPrincipal(id, "u@test.local"), "n/a", authorities));
    }
}
