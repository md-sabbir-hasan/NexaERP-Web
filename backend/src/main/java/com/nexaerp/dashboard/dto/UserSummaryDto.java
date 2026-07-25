package com.nexaerp.dashboard.dto;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSummaryDto {
    private Long total;

    private Long active;

    private Long pending;

    private Long inactive;

    private Long locked;
}
