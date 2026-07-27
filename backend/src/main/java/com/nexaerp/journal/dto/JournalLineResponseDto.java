package com.nexaerp.journal.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalLineResponseDto {
    private Long id;
    private Long accountId;
    private String accountName;
    private String accountCode;
    private Long costCenterId;
    private String costCenterCode;
    private String costCenterName;
    private BigDecimal debit;
    private BigDecimal credit;
    private String description;
}
