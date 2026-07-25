package com.nexaerp.debitnote;

import com.nexaerp.debitnote.dto.*;

import java.util.List;

public interface DebitNoteService {
    DebitNoteResponseDto create(DebitNoteRequestDto request);

    DebitNoteResponseDto update(Long id, DebitNoteRequestDto request);

    DebitNoteResponseDto getById(Long id);

    List<DebitNoteResponseDto> getAll();

    List<DebitNoteResponseDto> getByVendorBill(Long vendorBillId);

    DebitNoteResponseDto approve(Long id);

    DebitNoteResponseDto post(Long id);

    DebitNoteResponseDto cancel(Long id, DebitNoteCancelledReason reason);

    void delete(Long id);
}
