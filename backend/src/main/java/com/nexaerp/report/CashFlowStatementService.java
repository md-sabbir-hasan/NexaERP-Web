package com.nexaerp.report;

import com.nexaerp.report.dto.CashFlowStatementResponseDto;
import java.time.LocalDate;

public interface CashFlowStatementService {
    CashFlowStatementResponseDto generate(LocalDate fromDate, LocalDate toDate);
}
