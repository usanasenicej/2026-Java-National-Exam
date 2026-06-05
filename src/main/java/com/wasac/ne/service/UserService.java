package com.wasac.ne.service;

import com.wasac.ne.dto.request.CreateUserRequest;
import com.wasac.ne.dto.request.UpdateUserRequest;
import com.wasac.ne.dto.response.PageResponse;
import com.wasac.ne.dto.response.UserResponse;
import com.wasac.ne.entity.Customer;
import com.wasac.ne.entity.User;
import com.wasac.ne.enums.Status;
import com.wasac.ne.enums.UserRole;
import com.wasac.ne.exception.BusinessException;
import com.wasac.ne.exception.ResourceNotFoundException;
import com.wasac.ne.mapper.EntityMapper;
import com.wasac.ne.repository.CustomerRepository;
import com.wasac.ne.repository.UserRepository;
import com.wasac.ne.util.PasswordGenerator;
import com.wasac.ne.validation.StrongPasswordValidator;
import com.wasac.ne.validation.ValidEmailValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final OtpService otpService;
    private final EmailService emailService;
    private final ValidEmailValidator emailValidator = new ValidEmailValidator();
    private final StrongPasswordValidator passwordValidator = new StrongPasswordValidator();

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        boolean staffRole = isStaffRole(request.getRoles());
        String plainPassword = resolvePlainPassword(request, staffRole);

        validateEmail(request.getEmail(), request.getFullNames(), plainPassword, request.getPhoneNumber());

        if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new BusinessException("User with email '" + request.getEmail() + "' already exists");
        }

        boolean mustChangePassword = staffRole || request.getPassword() == null || request.getPassword().isBlank();

        User user = User.builder()
                .fullNames(request.getFullNames().trim())
                .email(request.getEmail().toLowerCase())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(plainPassword))
                .status(request.getStatus())
                .roles(new HashSet<>(request.getRoles()))
                .emailVerified(true)
                .mustChangePassword(mustChangePassword)
                .build();

        if (request.getCustomerId() != null) {
            Customer customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));
            user.setCustomer(customer);
        }

        user = userRepository.save(user);
        auditService.log("User", user.getId(), "CREATE", "Admin created user with roles: " + request.getRoles());

        if (mustChangePassword) {
            emailService.sendAccountCredentialsEmail(
                    user.getEmail(), user.getFullNames(), user.getEmail(), plainPassword, user.getRoles());
        } else {
            emailService.sendWelcomeEmail(user.getEmail(), user.getFullNames());
        }

        return EntityMapper.toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return EntityMapper.toUserResponse(findUser(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAll(String search, Status status, Pageable pageable) {
        Page<User> page;
        if (search != null && !search.isBlank()) {
            page = userRepository.findByFullNamesContainingIgnoreCaseOrEmailContainingIgnoreCase(
                    search, search, pageable);
        } else if (status != null) {
            page = userRepository.findByStatus(status, pageable);
        } else {
            page = userRepository.findAll(pageable);
        }
        return EntityMapper.toPageResponse(page, EntityMapper::toUserResponse);
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = findUser(id);
        Set<UserRole> previousRoles = new HashSet<>(user.getRoles());

        if (request.getFullNames() != null) user.setFullNames(request.getFullNames().trim());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());
        if (request.getStatus() != null) user.setStatus(request.getStatus());

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            user.setRoles(new HashSet<>(request.getRoles()));
        }

        if (request.getCustomerId() != null) {
            Customer customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
            user.setCustomer(customer);
        }

        user = userRepository.save(user);

        if (request.getRoles() != null && !request.getRoles().isEmpty()
                && !previousRoles.equals(user.getRoles())) {
            emailService.sendRoleChangeEmail(user.getEmail(), user.getFullNames(), previousRoles, user.getRoles());
            auditService.log("User", user.getId(), "ROLE_CHANGE",
                    "Roles changed from " + previousRoles + " to " + user.getRoles());
        } else {
            auditService.log("User", user.getId(), "UPDATE", "User updated");
        }

        return EntityMapper.toUserResponse(user);
    }

    @Transactional
    public void delete(Long id) {
        User user = findUser(id);
        userRepository.delete(user);
        auditService.log("User", id, "DELETE", "User deleted");
    }

    @Transactional
    public void resendVerificationOtp(Long id) {
        User user = findUser(id);
        if (user.isEmailVerified()) {
            throw new BusinessException("User email is already verified");
        }
        String otp = otpService.generateAndSaveOtp(user.getEmail(), com.wasac.ne.enums.OtpPurpose.REGISTRATION);
        emailService.sendOtpEmail(user.getEmail(), user.getFullNames(), otp, "account verification");
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    private boolean isStaffRole(Set<UserRole> roles) {
        return roles.stream().anyMatch(role ->
                role == UserRole.ROLE_ADMIN
                        || role == UserRole.ROLE_OPERATOR
                        || role == UserRole.ROLE_FINANCE);
    }

    private String resolvePlainPassword(CreateUserRequest request, boolean staffRole) {
        if (staffRole) {
            return PasswordGenerator.generateTemporaryPassword();
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            return PasswordGenerator.generateTemporaryPassword();
        }
        if (!passwordValidator.isValid(request.getPassword(), null)) {
            throw new BusinessException("Provided password does not meet strength requirements");
        }
        return request.getPassword();
    }

    private void validateEmail(String email, String fullNames, String password, String phone) {
        emailValidator.setCrossFieldValues(fullNames, password, phone);
        if (!emailValidator.isValid(email, null)) {
            throw new BusinessException("Invalid email format or email matches name/password/phone");
        }
    }
}
