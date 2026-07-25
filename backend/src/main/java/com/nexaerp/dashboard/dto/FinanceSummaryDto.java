package com.nexaerp.dashboard.dto;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinanceSummaryDto {
    private Long totalAccounts;

    private Long totalJournalEntries;

    private Long postedJournalEntries;

    private Long draftJournalEntries;

    private Long reversedJournalEntries;
}
