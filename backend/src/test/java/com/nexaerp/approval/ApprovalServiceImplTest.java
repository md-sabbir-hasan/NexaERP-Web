package com.nexaerp.approval;

import com.nexaerp.approval.dto.ApprovalDecisionDto;
import com.nexaerp.audit.AuditLogService;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.journal.*;
import com.nexaerp.notification.*;
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
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApprovalServiceImplTest {
    @Mock ApprovalRequestRepository requests; @Mock ApprovalActionRepository actions;
    @Mock JournalEntryRepository journals; @Mock UserRepository users; @Mock CurrentUserService currentUser;
    @Mock AuditLogService audit; @Mock NotificationService notifications;
    ApprovalProperties properties; ApprovalServiceImpl service;

    @BeforeEach void setUp(){
        properties=new ApprovalProperties();properties.setEnabled(true);properties.getManualJournal().setEnabled(true);
        service=new ApprovalServiceImpl(properties,requests,actions,
                List.of(new ManualJournalApprovalAdapter(properties,journals)),users,currentUser,audit,notifications);
        lenient().when(requests.saveAndFlush(any())).thenAnswer(i->{ApprovalRequest r=i.getArgument(0);r.setId(50L);return r;});
    }
    @AfterEach void clear(){SecurityContextHolder.clearContext();}

    @Test void successfulJournalSubmitNotifiesActiveApproversExceptMaker(){
        JournalEntry journal=journal(10L,1L);when(journals.findByIdForUpdate(10L)).thenReturn(Optional.of(journal));when(currentUser.getCurrentUserId()).thenReturn(1L);
        when(requests.findByEntityTypeAndEntityIdAndActiveMarker(any(),eq(10L),eq(1))).thenReturn(Optional.empty());
        when(requests.findTopByEntityTypeAndEntityIdOrderBySubmittedAtDesc(any(),eq(10L))).thenReturn(Optional.empty());
        when(users.findById(1L)).thenReturn(Optional.of(user(1L,UserStatus.ACTIVE)));
        when(users.findDistinctByStatusAndPermissionCode(UserStatus.ACTIVE,"APPROVE_JOURNAL"))
                .thenReturn(List.of(user(1L,UserStatus.ACTIVE),user(2L,UserStatus.ACTIVE)));
        authenticate(1L,"CREATE_JOURNAL");
        var result=service.submitManualJournal(10L);
        assertThat(result.getStatus()).isEqualTo(ApprovalStatus.PENDING);
        verify(notifications).scheduleUniqueForUsersAfterCommit(eq(List.of(2L)),eq(NotificationType.APPROVAL_SUBMITTED),
                eq(NotificationPriority.MEDIUM),eq(NotificationModule.APPROVAL),anyString(),contains("JE-0010"),eq("/approvals/50"),eq("APPROVAL_REQUEST"),eq(50L));
    }

    @Test void nonOwnerAndGeneratedJournalSubmissionsAreBlocked(){
        JournalEntry journal=journal(10L,1L);when(journals.findByIdForUpdate(10L)).thenReturn(Optional.of(journal));when(currentUser.getCurrentUserId()).thenReturn(9L);
        assertThatThrownBy(()->service.submitManualJournal(10L)).isInstanceOf(BusinessRuleException.class).hasMessageContaining("creator");
        journal.setSourceType(JournalSourceType.INVOICE);
        assertThatThrownBy(()->service.submitManualJournal(10L)).isInstanceOf(BusinessRuleException.class).hasMessageContaining("MANUAL");
    }

    @Test void approverCannotBeMakerAndMustBeActive(){
        ApprovalRequest request=pending(1L);JournalEntry journal=journal(10L,1L);when(requests.findById(50L)).thenReturn(Optional.of(request));when(requests.findByIdForUpdate(50L)).thenReturn(Optional.of(request));when(journals.findByIdForUpdate(10L)).thenReturn(Optional.of(journal));when(currentUser.getCurrentUserId()).thenReturn(1L);
        when(users.findById(1L)).thenReturn(Optional.of(user(1L,UserStatus.ACTIVE)));authenticate(1L,"APPROVE_JOURNAL");
        assertThatThrownBy(()->service.approve(50L,new ApprovalDecisionDto())).isInstanceOf(BusinessRuleException.class).hasMessageContaining("Maker");
        when(currentUser.getCurrentUserId()).thenReturn(2L);when(users.findById(2L)).thenReturn(Optional.of(user(2L,UserStatus.LOCKED)));authenticate(2L,"APPROVE_JOURNAL");
        assertThatThrownBy(()->service.approve(50L,new ApprovalDecisionDto())).isInstanceOf(BusinessRuleException.class).hasMessageContaining("ACTIVE");
    }

    @Test void approveKeepsJournalDraftAndRejectRequiresComment(){
        ApprovalRequest request=pending(1L);JournalEntry journal=journal(10L,1L);
        when(requests.findById(50L)).thenReturn(Optional.of(request));when(requests.findByIdForUpdate(50L)).thenReturn(Optional.of(request));when(currentUser.getCurrentUserId()).thenReturn(2L);
        when(users.findById(2L)).thenReturn(Optional.of(user(2L,UserStatus.ACTIVE)));when(journals.findByIdForUpdate(10L)).thenReturn(Optional.of(journal));authenticate(2L,"APPROVE_JOURNAL");
        service.approve(50L,new ApprovalDecisionDto());
        assertThat(request.getStatus()).isEqualTo(ApprovalStatus.APPROVED);assertThat(request.getActiveMarker()).isEqualTo(1);assertThat(journal.getStatus()).isEqualTo(JournalStatus.DRAFT);
        ApprovalDecisionDto empty=new ApprovalDecisionDto();assertThatThrownBy(()->service.reject(50L,empty)).isInstanceOf(BusinessRuleException.class).hasMessageContaining("comment");
    }

    @Test void staleJournalBlocksDecisionAndSuccessfulConsumptionClearsActiveMarker(){
        ApprovalRequest request=pending(1L);JournalEntry journal=journal(10L,1L);journal.setUpdatedAt(request.getDocumentUpdatedAt().plusSeconds(1));
        when(requests.findById(50L)).thenReturn(Optional.of(request));when(requests.findByIdForUpdate(50L)).thenReturn(Optional.of(request));when(currentUser.getCurrentUserId()).thenReturn(2L);
        when(users.findById(2L)).thenReturn(Optional.of(user(2L,UserStatus.ACTIVE)));when(journals.findByIdForUpdate(10L)).thenReturn(Optional.of(journal));authenticate(2L,"APPROVE_JOURNAL");
        assertThatThrownBy(()->service.approve(50L,new ApprovalDecisionDto())).isInstanceOf(BusinessRuleException.class).hasMessageContaining("changed");
        request.setStatus(ApprovalStatus.APPROVED);when(users.findById(2L)).thenReturn(Optional.of(user(2L,UserStatus.ACTIVE)));
        service.consumeAfterSuccessfulPost(request);
        assertThat(request.getConsumedAt()).isNotNull();assertThat(request.getActiveMarker()).isNull();verify(actions).save(argThat(a->a.getAction()==ApprovalActionType.CONSUMED));
    }

    private ApprovalRequest pending(Long maker){return ApprovalRequest.builder().id(50L).entityType(ApprovalEntityType.MANUAL_JOURNAL).entityId(10L)
            .documentNumber("JE-0010").makerUserId(maker).status(ApprovalStatus.PENDING).requiredPermission("APPROVE_JOURNAL")
            .documentUpdatedAt(LocalDateTime.of(2026,8,2,10,0)).submittedAt(LocalDateTime.now()).activeMarker(1).build();}
    private JournalEntry journal(Long id,Long maker){JournalEntry j=JournalEntry.builder().id(id).entryNumber("JE-0010").sourceType(JournalSourceType.MANUAL).status(JournalStatus.DRAFT).build();
        j.setCreatedBy(maker);j.setUpdatedAt(LocalDateTime.of(2026,8,2,10,0));JournalLine d=JournalLine.builder().debit(BigDecimal.TEN).credit(BigDecimal.ZERO).build();JournalLine c=JournalLine.builder().debit(BigDecimal.ZERO).credit(BigDecimal.TEN).build();j.setLines(List.of(d,c));return j;}
    private User user(Long id,UserStatus status){return User.builder().id(id).name("User "+id).email("u"+id+"@test.local").status(status).build();}
    private void authenticate(Long id,String... permissions){var authorities=Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new CurrentUserPrincipal(id,"u@test.local"),"n/a",authorities));}
}
