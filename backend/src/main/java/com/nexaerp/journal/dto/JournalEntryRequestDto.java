package com.nexaerp.journal.dto;

import com.nexaerp.journal.JournalEntryType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntryRequestDto {
    @NotNull(message = "Date is required")
    private LocalDate date;

    private String description;

    @NotNull(message = "Type is required")
    private JournalEntryType type;

    @NotNull(message = "Lines are required")
    @Size(min = 2, message = "At least 2 lines required")
    private List<JournalLineRequestDto> lines;
}
