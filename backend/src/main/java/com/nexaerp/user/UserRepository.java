package com.nexaerp.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>{
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByInviteToken(String inviteToken);


    Page<User> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String name,
            String email,
            Pageable pageable
    );

    Page<User> findByStatus(
            UserStatus status,
            Pageable pageable
    );

    Page<User> findByStatusAndNameContainingIgnoreCaseOrStatusAndEmailContainingIgnoreCase(
            UserStatus status1,
            String name,
            UserStatus status2,
            String email,
            Pageable pageable
    );

    long countByRoles_Id(Long roleId);

    long countByStatus(UserStatus status);
}
