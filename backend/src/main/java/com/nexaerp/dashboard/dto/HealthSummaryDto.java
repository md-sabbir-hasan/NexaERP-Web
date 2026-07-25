package com.nexaerp.dashboard.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthSummaryDto {
    private String database;
    private String mail;
    private String application;
}
