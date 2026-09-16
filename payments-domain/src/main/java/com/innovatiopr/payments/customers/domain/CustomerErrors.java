package com.innovatiopr.payments.customers.domain;

import com.innovatiopr.payments.customers.CustomerId;
import com.innovatiopr.payments.shared.domain.ConflictException;
import com.innovatiopr.payments.shared.domain.DomainException;
import com.innovatiopr.payments.shared.domain.NotFoundException;
import com.innovatiopr.payments.shared.domain.ValidationException;

/** Expected business failures owned by the Customers bounded context. */
public final class CustomerErrors {

    private CustomerErrors() {
    }

    public static NotFoundException notFound(CustomerId id) {
        return new NotFoundException("CUSTOMER_NOT_FOUND", "No customer exists with id " + id.value());
    }

    public static ConflictException emailAlreadyRegistered(String email) {
        return new ConflictException(
                "CUSTOMER_EMAIL_ALREADY_REGISTERED",
                "A customer is already registered with email " + email);
    }

    public static ValidationException invalidEmail(String reason) {
        return new ValidationException("CUSTOMER_INVALID_EMAIL", reason);
    }

    public static ValidationException invalidName(String reason) {
        return new ValidationException("CUSTOMER_INVALID_NAME", reason);
    }
}
