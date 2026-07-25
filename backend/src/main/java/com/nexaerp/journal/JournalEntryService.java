package com.nexaerp.journal;

import com.nexaerp.journal.dto.JournalEntryRequestDto;
import com.nexaerp.journal.dto.JournalEntryResponseDto;

import java.util.List;

public interface JournalEntryService {
    JournalEntryResponseDto create(JournalEntryRequestDto request);

    JournalEntryResponseDto update(Long id, JournalEntryRequestDto request);

    JournalEntryResponseDto getById(Long id);

    List<JournalEntryResponseDto> getAll();

    JournalEntryResponseDto post(Long id);      // DRAFT to POSTED

    JournalEntryResponseDto reverse(Long id);   // POSTED to REVERSED

    void delete(Long id);                        // only DRAFT

}
