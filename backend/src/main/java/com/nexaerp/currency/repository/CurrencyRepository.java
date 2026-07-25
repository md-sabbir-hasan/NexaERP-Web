package com.nexaerp.currency.repository;

import com.nexaerp.currency.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CurrencyRepository extends JpaRepository<Currency, Long> {

    Optional<Currency> findByCodeIgnoreCase(String code);

    Optional<Currency> findByBaseCurrencyTrue();

    List<Currency> findByActiveTrueOrderByCodeAsc();

    boolean existsByCodeIgnoreCase(String code);
}