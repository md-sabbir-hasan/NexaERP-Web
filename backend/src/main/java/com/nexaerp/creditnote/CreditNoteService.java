package com.nexaerp.creditnote;
import com.nexaerp.creditnote.dto.*;
import java.util.List;
public interface CreditNoteService {
    CreditNoteResponseDto create(CreditNoteRequestDto request);
    CreditNoteResponseDto update(Long id,CreditNoteRequestDto request);
    CreditNoteResponseDto getById(Long id);
    List<CreditNoteResponseDto> getAll();
    List<CreditNoteResponseDto> getByInvoice(Long invoiceId);
    CreditNoteResponseDto approve(Long id);
    CreditNoteResponseDto post(Long id);
    CreditNoteResponseDto cancel(Long id,CreditNoteCancelledReason reason);
    void delete(Long id);
}
