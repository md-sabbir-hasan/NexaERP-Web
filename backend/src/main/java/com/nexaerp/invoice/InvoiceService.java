package com.nexaerp.invoice;

import com.nexaerp.invoice.dto.InvoiceRequestDto;
import com.nexaerp.invoice.dto.InvoiceResponseDto;
import com.nexaerp.fileupload.dto.FileUploadResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface InvoiceService {
    InvoiceResponseDto create(InvoiceRequestDto request);
    InvoiceResponseDto update(Long id, InvoiceRequestDto request);
    InvoiceResponseDto getById(Long id);
    List<InvoiceResponseDto> getAll();
    List<InvoiceResponseDto> getByParty(Long partyId);
    List<InvoiceResponseDto> getByStatus(InvoiceStatus status);
    InvoiceResponseDto post(Long id);
    InvoiceResponseDto cancel(Long id, CancelledReason reason);
    FileUploadResponseDto uploadAttachment(Long id, MultipartFile file);
}
