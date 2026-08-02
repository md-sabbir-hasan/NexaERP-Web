package com.nexaerp.journal.dto;

import com.nexaerp.journal.JournalEntryType;
import com.nexaerp.journal.JournalSourceType;
import com.nexaerp.journal.JournalStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import com.nexaerp.approval.ApprovalStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalEntryResponseDto {
    private Long id;
    private String entryNumber;
    private LocalDate date;
    private String description;
    private JournalEntryType type;
    private JournalStatus status;
    private JournalSourceType sourceType;
    private BigDecimal totalAmount;
    private List<JournalLineResponseDto> lines;
    private Long createdBy;
    private Boolean approvalEnabled;
    private Long approvalRequestId;
    private ApprovalStatus approvalStatus;
}
