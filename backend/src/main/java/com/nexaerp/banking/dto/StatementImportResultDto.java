package com.nexaerp.banking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatementImportResultDto {
    private int totalLines;
    private int autoMatchedCount;
    private int unmatchedCount;
    private List<BankStatementLineResponseDto> lines;
    private BankReconciliationResponseDto reconciliation;
}
