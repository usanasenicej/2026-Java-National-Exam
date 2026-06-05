package com.wasac.ne.repository;

import com.wasac.ne.entity.User;
import com.wasac.ne.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNationalId(String nationalId);

    Page<User> findByFullNamesContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String fullNames, String email, Pageable pageable);

    Page<User> findByStatus(Status status, Pageable pageable);
}
