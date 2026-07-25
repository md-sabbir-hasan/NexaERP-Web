package com.nexaerp.expense.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseAttachmentRequestDto {
    @NotBlank(message = "attachmentUrl is required")
    private String attachmentUrl;
}