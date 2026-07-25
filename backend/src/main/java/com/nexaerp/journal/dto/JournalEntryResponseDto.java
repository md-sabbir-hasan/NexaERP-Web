package com.nexaerp.journal.dto;

import com.nexaerp.journal.JournalEntryType;
import com.nexaerp.journal.JournalSourceType;
import com.nexaerp.journal.JournalStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalEntryResponseDto {
    private Long id;
    private String entryNumber;
    private LocalDate date;
    private String description;
    private JournalEntryType type;
    private JournalStatus status;
    private JournalSourceType sourceType;
    private BigDecimal totalAmount;
    private List<JournalLineResponseDto> lines;
}
