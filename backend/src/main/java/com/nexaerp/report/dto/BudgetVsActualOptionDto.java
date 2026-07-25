package com.nexaerp.report.dto;

import com.nexaerp.budget.BudgetStatus;
import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BudgetVsActualOptionDto {
    private Long budgetId;
    private String budgetNumber;
    private String budgetName;
    private BudgetStatus budgetStatus;
    private Long fiscalYearId;
    private String fiscalYearName;
    private List<BudgetVsActualPeriodOptionDto> periods;
}
