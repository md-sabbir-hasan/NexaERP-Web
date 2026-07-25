package com.nexaerp.vendorbill;

import com.nexaerp.vendorbill.dto.VendorBillRequestDto;
import com.nexaerp.vendorbill.dto.VendorBillResponseDto;

import java.util.List;

public interface VendorBillService {
    VendorBillResponseDto create(VendorBillRequestDto request);

    VendorBillResponseDto update(Long id, VendorBillRequestDto request);

    VendorBillResponseDto getById(Long id);

    List<VendorBillResponseDto> getAll();

    List<VendorBillResponseDto> getByParty(Long partyId);

    List<VendorBillResponseDto> getByStatus(VendorBillStatus status);

    List<VendorBillResponseDto> getByBillType(VendorBillType billType);

    VendorBillResponseDto approve(Long id);

    VendorBillResponseDto post(Long id);

    VendorBillResponseDto cancel(Long id, VendorBillCancelledReason reason);

}
