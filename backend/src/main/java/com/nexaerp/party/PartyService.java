package com.nexaerp.party;

import com.nexaerp.party.dto.PartyRequestDto;
import com.nexaerp.party.dto.PartyResponseDto;

import java.util.List;

public interface PartyService {
    PartyResponseDto create(PartyRequestDto request);
    PartyResponseDto update(Long id, PartyRequestDto request);
    PartyResponseDto getById(Long id);
    List<PartyResponseDto> getAll();
    List<PartyResponseDto> getByType(PartyType type);
    void deactivate(Long id);
    void activate(Long id);
}
