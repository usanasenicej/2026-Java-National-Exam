package com.wasac.ne.repository;

import com.wasac.ne.entity.Customer;
import com.wasac.ne.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByNationalId(String nationalId);

    boolean existsByEmail(String email);

    Optional<Customer> findByNationalId(String nationalId);

    Optional<Customer> findByEmail(String email);

    Page<Customer> findByFullNamesContainingIgnoreCaseOrNationalIdContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String fullNames, String nationalId, String email, Pageable pageable);

    Page<Customer> findByStatus(Status status, Pageable pageable);
}
