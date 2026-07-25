package com.nexaerp.fiscalyear.dto;

import com.nexaerp.fiscalyear.FiscalYearStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FiscalYearResponseDto {
    private Long id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private FiscalYearStatus status;
    private String description;
    private boolean current;
    private LocalDateTime activatedAt;
    private Long activatedBy;
    private LocalDateTime closedAt;
    private Long closedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
