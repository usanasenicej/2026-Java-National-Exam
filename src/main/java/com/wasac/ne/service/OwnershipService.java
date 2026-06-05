package com.wasac.ne.service;

import com.wasac.ne.entity.User;
import com.wasac.ne.exception.BusinessException;
import com.wasac.ne.repository.UserRepository;
import com.wasac.ne.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Centralises customer-ownership scoping.
 *
 * ROLE_CUSTOMER users may only access resources that belong to their own
 * linked Customer record.  All other roles (ADMIN, FINANCE, OPERATOR) have
 * unrestricted access and bypass these checks.
 */
@Service
@RequiredArgsConstructor
public class OwnershipService {

    private final UserRepository userRepository;

    /**
     * Returns the customer ID that is linked to the currently-authenticated user,
     * or {@code null} if the current user is not a ROLE_CUSTOMER (i.e. has an
     * elevated role and should not be restricted).
     *
     * <p>Throws {@link BusinessException} when:
     * <ul>
     *   <li>The current principal is ROLE_CUSTOMER but has no linked Customer record.</li>
     * </ul>
     */
    public Long getOwnedCustomerIdOrNull() {
        if (!SecurityUtils.isCustomerOnly()) {
            // ADMIN / FINANCE / OPERATOR — no restriction
            return null;
        }

        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Authenticated user not found: " + email));

        if (user.getCustomer() == null) {
            throw new BusinessException(
                    "Your user account is not linked to a customer profile. " +
                    "Please contact an administrator.");
        }
        return user.getCustomer().getId();
    }

    /**
     * Enforces that a ROLE_CUSTOMER can only access data for their own customer.
     *
     * <p>If the current principal is ROLE_CUSTOMER and {@code resourceCustomerId}
     * does not match their own customer ID, an {@link BusinessException} is thrown.
     * Non-customer roles pass through silently.
     *
     * @param resourceCustomerId the customer ID that owns the resource being accessed
     * @param resourceDescription a short description used in the error message (e.g. "Bill", "Meter")
     */
    public void assertOwnership(Long resourceCustomerId, String resourceDescription) {
        Long ownedId = getOwnedCustomerIdOrNull();
        if (ownedId == null) return; // elevated role — no restriction
        if (!ownedId.equals(resourceCustomerId)) {
            throw new BusinessException(
                    "Access denied: you are not authorised to access this " + resourceDescription + ".");
        }
    }
}
