package com.nexaerp.accountingperiod.checklist;

import com.nexaerp.accountingperiod.dto.PeriodCloseCheckItemDto;
import com.nexaerp.accountingperiod.dto.PeriodCloseCheckResultDto;
import com.nexaerp.journal.JournalEntry;
import com.nexaerp.journal.JournalEntryRepository;
import com.nexaerp.journal.JournalStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UnpostedJournalCheck implements PeriodCloseCheck {

    private final JournalEntryRepository journalEntryRepository;

    @Override
    public String getCode() {
        return "UNPOSTED_JOURNAL";
    }

    @Override
    public String getName() {
        return "Unposted Journal";
    }

    @Override
    public PeriodCloseCheckResultDto run(LocalDate periodEndDate) {
        List<JournalEntry> drafts =
                journalEntryRepository.findByStatusAndDateLessThanEqual(JournalStatus.DRAFT, periodEndDate);

        List<PeriodCloseCheckItemDto> items = drafts.stream()
                .map(j -> PeriodCloseCheckItemDto.builder()
                        .id(j.getId())
                        .reference(j.getEntryNumber())
                        .date(j.getDate())
                        .amount(j.getTotalAmount())
                        .build())
                .toList();

        return PeriodCloseCheckResultDto.builder()
                .code(getCode())
                .name(getName())
                .passed(items.isEmpty())
                .count(items.size())
                .items(items)
                .build();
    }
}