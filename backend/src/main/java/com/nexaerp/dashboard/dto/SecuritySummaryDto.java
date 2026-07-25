package com.nexaerp.dashboard.dto;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecuritySummaryDto {
    private Long totalRoles;

    private Long totalPermissions;
}
