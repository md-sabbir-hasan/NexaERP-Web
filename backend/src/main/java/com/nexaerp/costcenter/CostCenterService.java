package com.nexaerp.costcenter;

import com.nexaerp.costcenter.dto.CostCenterLookupDto;
import com.nexaerp.costcenter.dto.CostCenterRequestDto;
import com.nexaerp.costcenter.dto.CostCenterResponseDto;

import java.util.List;

public interface CostCenterService {
    CostCenterResponseDto create(CostCenterRequestDto request);
    CostCenterResponseDto update(Long id, CostCenterRequestDto request);
    CostCenterResponseDto getById(Long id);
    List<CostCenterResponseDto> getAll();
    List<CostCenterResponseDto> search(String keyword, Boolean active);
    List<CostCenterLookupDto> lookup();
    void activate(Long id);
    void deactivate(Long id);
    CostCenter resolveActive(Long id);
}
