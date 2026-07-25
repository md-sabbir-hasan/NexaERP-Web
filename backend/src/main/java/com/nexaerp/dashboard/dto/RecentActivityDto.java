package com.nexaerp.dashboard.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class RecentActivityDto {
    private String action;
    private String entityName;
    private Long entityId;
    private String userName;
    private LocalDateTime createdAt;
    private String description;
}
