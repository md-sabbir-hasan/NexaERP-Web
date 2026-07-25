package com.nexaerp.fixedasset.repository;

import com.nexaerp.fixedasset.DepreciationEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepreciationEntryRepository extends JpaRepository<DepreciationEntry, Long> {
    List<DepreciationEntry> findByFixedAssetIdOrderByPeriodDateDesc(Long fixedAssetId);
}
