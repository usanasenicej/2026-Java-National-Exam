package com.wasac.ne.service;

import com.wasac.ne.dto.request.CreateCustomerRequest;
import com.wasac.ne.dto.request.UpdateCustomerRequest;
import com.wasac.ne.dto.response.CustomerResponse;
import com.wasac.ne.dto.response.PageResponse;
import com.wasac.ne.entity.Customer;
import com.wasac.ne.enums.Status;
import com.wasac.ne.exception.BusinessException;
import com.wasac.ne.exception.ResourceNotFoundException;
import com.wasac.ne.mapper.EntityMapper;
import com.wasac.ne.repository.CustomerRepository;
import com.wasac.ne.validation.ValidEmailValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final AuditService auditService;
    private final ValidEmailValidator emailValidator = new ValidEmailValidator();

    @Transactional
    public CustomerResponse create(CreateCustomerRequest request) {
        validateEmail(request.getEmail());

        if (customerRepository.existsByNationalId(request.getNationalId())) {
            throw new BusinessException("Customer with National ID '" + request.getNationalId() + "' already exists");
        }
        if (customerRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new BusinessException("Customer with email '" + request.getEmail() + "' already exists");
        }
        if (customerRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BusinessException("Customer with phone number '" + request.getPhoneNumber() + "' already exists");
        }

        if (request.getDateOfBirth() != null) {
            validateAge(request.getDateOfBirth());
        }

        Customer customer = Customer.builder()
                .fullNames(request.getFullNames().trim())
                .nationalId(request.getNationalId())
                .email(request.getEmail().toLowerCase())
                .phoneNumber(request.getPhoneNumber())
                .dateOfBirth(request.getDateOfBirth())
                .address(request.getAddress().trim())
                .status(request.getStatus())
                .build();

        customer = customerRepository.save(customer);
        auditService.log("Customer", customer.getId(), "CREATE",
                null,
                "nationalId=" + customer.getNationalId() + ",email=" + customer.getEmail(),
                "Customer registered: " + customer.getFullNames());
        return EntityMapper.toCustomerResponse(customer);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getById(Long id) {
        return EntityMapper.toCustomerResponse(findCustomer(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> getAll(String search, Status status, Pageable pageable) {
        Page<Customer> page;
        boolean hasSearch = search != null && !search.isBlank();
        boolean hasStatus = status != null;

        if (hasSearch && hasStatus) {
            // Both filters — combined query
            page = customerRepository.findByStatusAndSearch(status, search, pageable);
        } else if (hasSearch) {
            // Search across fullNames, nationalId, email
            page = customerRepository
                    .findByFullNamesContainingIgnoreCaseOrNationalIdContainingIgnoreCaseOrEmailContainingIgnoreCase(
                            search, search, search, pageable);
        } else if (hasStatus) {
            // Status filter only
            page = customerRepository.findByStatus(status, pageable);
        } else {
            // No filters — return all
            page = customerRepository.findAll(pageable);
        }
        return EntityMapper.toPageResponse(page, EntityMapper::toCustomerResponse);
    }

    @Transactional
    public CustomerResponse update(Long id, UpdateCustomerRequest request) {
        Customer customer = findCustomer(id);

        // Capture snapshot before changes for audit
        String oldSnapshot = "fullNames=" + customer.getFullNames()
                + ",email=" + customer.getEmail()
                + ",phone=" + customer.getPhoneNumber()
                + ",status=" + customer.getStatus();

        if (request.getFullNames() != null) customer.setFullNames(request.getFullNames().trim());
        if (request.getEmail() != null) {
            validateEmail(request.getEmail());
            if (customerRepository.existsByEmail(request.getEmail().toLowerCase())
                    && !customer.getEmail().equalsIgnoreCase(request.getEmail())) {
                throw new BusinessException("Email '" + request.getEmail() + "' is already in use");
            }
            customer.setEmail(request.getEmail().toLowerCase());
        }
        if (request.getPhoneNumber() != null) {
            if (customerRepository.existsByPhoneNumber(request.getPhoneNumber())
                    && !customer.getPhoneNumber().equals(request.getPhoneNumber())) {
                throw new BusinessException("Phone number '" + request.getPhoneNumber() + "' is already in use");
            }
            customer.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getDateOfBirth() != null) {
            validateAge(request.getDateOfBirth());
            customer.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getAddress() != null) customer.setAddress(request.getAddress().trim());
        if (request.getStatus() != null) customer.setStatus(request.getStatus());

        customer = customerRepository.save(customer);

        String newSnapshot = "fullNames=" + customer.getFullNames()
                + ",email=" + customer.getEmail()
                + ",phone=" + customer.getPhoneNumber()
                + ",status=" + customer.getStatus();

        auditService.log("Customer", customer.getId(), "UPDATE",
                oldSnapshot, newSnapshot, "Customer updated");
        return EntityMapper.toCustomerResponse(customer);
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = findCustomer(id);
        auditService.log("Customer", id, "DELETE",
                "nationalId=" + customer.getNationalId() + ",email=" + customer.getEmail(), null,
                "Customer deleted: " + customer.getFullNames());
        customerRepository.delete(customer);
    }

    public Customer findCustomer(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
    }

    public void ensureCustomerActive(Customer customer) {
        if (customer.getStatus() == Status.INACTIVE || customer.getStatus() == Status.SUSPENDED) {
            throw new BusinessException("Cannot bill inactive or suspended customer: " + customer.getFullNames());
        }
    }

    private void validateEmail(String email) {
        if (!emailValidator.isValid(email, null)) {
            throw new BusinessException("Invalid email format. Email must be lowercase and valid.");
        }
    }

    private void validateAge(LocalDate dateOfBirth) {
        int age = Period.between(dateOfBirth, LocalDate.now()).getYears();
        if (age < 18) {
            throw new BusinessException("Customer must be at least 18 years old. Current age: " + age);
        }
    }
}
