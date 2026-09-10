package com.innovatiopr.payments.customers.domain;

import com.innovatiopr.payments.customers.CustomerId;
import com.innovatiopr.payments.shared.domain.DomainError;
import com.innovatiopr.payments.shared.domain.ErrorType;

/** Expected business failures owned by the Customers bounded context. */
public sealed interface CustomerError extends DomainError {

    record NotFound(CustomerId customerId) implements CustomerError {
        @Override
        public String code() {
            return "CUSTOMER_NOT_FOUND";
        }

        @Override
        public String message() {
            return "No customer exists with id " + customerId.value();
        }

        @Override
        public ErrorType type() {
            return ErrorType.NOT_FOUND;
        }
    }

    record EmailAlreadyRegistered(String email) implements CustomerError {
        @Override
        public String code() {
            return "CUSTOMER_EMAIL_ALREADY_REGISTERED";
        }

        @Override
        public String message() {
            return "A customer is already registered with email " + email;
        }

        @Override
        public ErrorType type() {
            return ErrorType.CONFLICT;
        }
    }

    record InvalidEmail(String reason) implements CustomerError {
        @Override
        public String code() {
            return "CUSTOMER_INVALID_EMAIL";
        }

        @Override
        public String message() {
            return reason;
        }

        @Override
        public ErrorType type() {
            return ErrorType.VALIDATION;
        }
    }

    record InvalidName(String reason) implements CustomerError {
        @Override
        public String code() {
            return "CUSTOMER_INVALID_NAME";
        }

        @Override
        public String message() {
            return reason;
        }

        @Override
        public ErrorType type() {
            return ErrorType.VALIDATION;
        }
    }

    static CustomerError notFound(CustomerId id) {
        return new NotFound(id);
    }

    static CustomerError emailAlreadyRegistered(String email) {
        return new EmailAlreadyRegistered(email);
    }

    static CustomerError invalidEmail(String reason) {
        return new InvalidEmail(reason);
    }

    static CustomerError invalidName(String reason) {
        return new InvalidName(reason);
    }
}
