package com.nexaerp.fixedasset.services;

import com.nexaerp.fixedasset.dto.*;

import java.time.LocalDate;
import java.util.List;

public interface FixedAssetService {

    FixedAssetResponseDto create(FixedAssetRequestDto request);

    FixedAssetResponseDto getById(Long id);

    List<FixedAssetResponseDto> getAll();

    List<DepreciationEntryResponseDto> getDepreciationHistory(Long fixedAssetId);

    // Run depreciation for one asset, up to asOfDate. No-op (returns null-ish/no entry) if not yet due.
    DepreciationEntryResponseDto runDepreciation(Long fixedAssetId, LocalDate asOfDate);

    // Run depreciation for every ACTIVE asset that is due, up to asOfDate
    List<DepreciationEntryResponseDto> runDepreciationForAll(LocalDate asOfDate);

    FixedAssetResponseDto dispose(Long fixedAssetId, AssetDisposalRequestDto request);
}
