package com.nexaerp.party;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartyRepository extends JpaRepository<Party, Long> {
    boolean existsByCode(String code);
    boolean existsByPhone(String phone);
    Optional<Party> findByCode(String code);
    List<Party> findByType(PartyType type);
    List<Party> findByIsActive(Boolean isActive);
    List<Party> findByTypeOrType(PartyType type1, PartyType type2);
}
