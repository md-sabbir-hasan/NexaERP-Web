package com.nexaerp.report.dto;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BudgetVsActualPeriodOptionDto {
    private Long id;
    private String name;
    private Integer periodNumber;
    private LocalDate startDate;
    private LocalDate endDate;
}
