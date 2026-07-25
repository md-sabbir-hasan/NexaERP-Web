package com.nexaerp.fixedasset.repository;

import com.nexaerp.fixedasset.AssetStatus;
import com.nexaerp.fixedasset.FixedAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FixedAssetRepository extends JpaRepository<FixedAsset, Long> {
    List<FixedAsset> findByStatus(AssetStatus status);
    Optional<FixedAsset> findTopByOrderByIdDesc();
    boolean existsByAssetCode(String assetCode);
}
