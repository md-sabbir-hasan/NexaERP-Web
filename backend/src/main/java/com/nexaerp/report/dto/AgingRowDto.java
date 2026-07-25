package com.nexaerp.report.dto;


import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgingRowDto {
    private Long partyId;
    private String partyName;
    private BigDecimal current;     // not yet due
    private BigDecimal days1to30;
    private BigDecimal days31to60;
    private BigDecimal days61to90;
    private BigDecimal days91Plus;
    private BigDecimal totalDue;
}
