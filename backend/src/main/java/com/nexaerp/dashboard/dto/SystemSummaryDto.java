package com.nexaerp.dashboard.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSummaryDto {
    private String applicationVersion;
    private LocalDateTime serverTime;
    private String serverTimezone;
    private String environment;
    private String javaVersion;
}
