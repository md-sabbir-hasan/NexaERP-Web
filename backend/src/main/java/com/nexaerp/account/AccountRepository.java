package com.nexaerp.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    boolean existsByCode(String code);

    Optional<Account> findByCode(String code);

    List<Account> findByType(AccountType type);

    List<Account> findByParentIsNull(); // Root accounts

    List<Account> findByIsActive(Boolean isActive);

    boolean existsByParentId(Long parentId);

    List<Account> findByTypeAndIsActive(AccountType type, Boolean isActive);

    List<Account> findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(
            String name,
            String code
    );

    List<Account> findByTypeAndNameContainingIgnoreCaseOrTypeAndCodeContainingIgnoreCase(
            AccountType type1,
            String name,
            AccountType type2,
            String code
    );

    List<Account> findByIsActiveAndNameContainingIgnoreCaseOrIsActiveAndCodeContainingIgnoreCase(
            Boolean active1,
            String name,
            Boolean active2,
            String code
    );

    List<Account> findByTypeAndIsActiveAndNameContainingIgnoreCaseOrTypeAndIsActiveAndCodeContainingIgnoreCase(
            AccountType type1,
            Boolean active1,
            String name,
            AccountType type2,
            Boolean active2,
            String code
    );
}
