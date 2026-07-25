package com.nexaerp.payment;

import com.nexaerp.payment.dto.PartyOutstandingSummaryDto;
import com.nexaerp.payment.dto.PaymentRequestDto;
import com.nexaerp.payment.dto.PaymentResponseDto;

import java.util.List;

public interface PaymentService {
    // Create a new payment in DRAFT status (with auto or manual allocation saved)
    PaymentResponseDto create(PaymentRequestDto request);

    PaymentResponseDto getById(Long id);

    List<PaymentResponseDto> getAll();

    List<PaymentResponseDto> getByParty(Long partyId);

    PaymentResponseDto post(Long id);

    // Reverses journal entry and undo invoice/bill due amount changes if already posted
    PaymentResponseDto cancel(Long id);

    // Returns customer invoice or vendor bill outstanding summary
    PartyOutstandingSummaryDto getOutstandingSummary(
            Long partyId,
            PaymentType paymentType
    );
}
