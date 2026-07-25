package com.nexaerp.accountingperiod.dto;

import com.nexaerp.accountingperiod.AccountingPeriodStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountingPeriodResponseDto {
    private Long id;
    private Long fiscalYearId;
    private String fiscalYearName;
    private String name;
    private Integer periodNumber;
    private LocalDate startDate;
    private LocalDate endDate;
    private AccountingPeriodStatus status;
    private boolean future;
    private boolean current;
    private LocalDateTime closedAt;
    private Long closedBy;
    private LocalDateTime lockedAt;
    private Long lockedBy;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
