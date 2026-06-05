package com.wasac.ne.repository;

import com.wasac.ne.entity.Customer;
import com.wasac.ne.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByNationalId(String nationalId);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    Optional<Customer> findByNationalId(String nationalId);

    Optional<Customer> findByEmail(String email);

    // Search only (no status filter)
    Page<Customer> findByFullNamesContainingIgnoreCaseOrNationalIdContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String fullNames, String nationalId, String email, Pageable pageable);

    // Status only (no search filter)
    Page<Customer> findByStatus(Status status, Pageable pageable);

    // Combined: search + status filter
    @Query("SELECT c FROM Customer c WHERE c.status = :status AND (" +
           "LOWER(c.fullNames) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "c.nationalId LIKE CONCAT('%', :search, '%') OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Customer> findByStatusAndSearch(
            @Param("status") Status status,
            @Param("search") String search,
            Pageable pageable);
}
