package com.nexaerp.approval.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ApprovalDecisionDto {
    @Size(max = 500)
    private String comment;
}
