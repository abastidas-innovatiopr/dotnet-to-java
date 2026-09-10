package com.innovatiopr.payments.accounts.domain;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.shared.domain.DomainError;
import com.innovatiopr.payments.shared.domain.ErrorType;
import com.innovatiopr.payments.shared.domain.Money;

/**
 * Expected business failures owned by the Accounts bounded context.
 *
 * <p>None of these mention HTTP. {@code InsufficientFunds} does not know that the API renders it as 422;
 * it only classifies itself as a {@link ErrorType#BUSINESS_RULE}.
 */
public sealed interface AccountError extends DomainError {

    record NotFound(AccountId accountId) implements AccountError {
        @Override
        public String code() {
            return "ACCOUNT_NOT_FOUND";
        }

        @Override
        public String message() {
            return "No account exists with id " + accountId.value();
        }

        @Override
        public ErrorType type() {
            return ErrorType.NOT_FOUND;
        }
    }

    record NotActive(AccountId accountId, AccountStatus status) implements AccountError {
        @Override
        public String code() {
            return "ACCOUNT_NOT_ACTIVE";
        }

        @Override
        public String message() {
            return "Account %s is %s and cannot transact".formatted(accountId.value(), status);
        }

        @Override
        public ErrorType type() {
            return ErrorType.BUSINESS_RULE;
        }
    }

    record Frozen(AccountId accountId) implements AccountError {
        @Override
        public String code() {
            return "ACCOUNT_FROZEN";
        }

        @Override
        public String message() {
            return "Account %s is frozen and can neither send nor receive money".formatted(accountId.value());
        }

        @Override
        public ErrorType type() {
            return ErrorType.BUSINESS_RULE;
        }
    }

    record Closed(AccountId accountId) implements AccountError {
        @Override
        public String code() {
            return "ACCOUNT_CLOSED";
        }

        @Override
        public String message() {
            return "Account %s is closed".formatted(accountId.value());
        }

        @Override
        public ErrorType type() {
            return ErrorType.BUSINESS_RULE;
        }
    }

    record InsufficientFunds(AccountId accountId, Money requested, Money available) implements AccountError {
        @Override
        public String code() {
            return "ACCOUNT_INSUFFICIENT_FUNDS";
        }

        @Override
        public String message() {
            return "Account %s has %s available but %s was requested"
                    .formatted(accountId.value(), available, requested);
        }

        @Override
        public ErrorType type() {
            return ErrorType.BUSINESS_RULE;
        }
    }

    record NotEmpty(AccountId accountId, Money balance) implements AccountError {
        @Override
        public String code() {
            return "ACCOUNT_NOT_EMPTY";
        }

        @Override
        public String message() {
            return "Account %s cannot be closed while it holds %s".formatted(accountId.value(), balance);
        }

        @Override
        public ErrorType type() {
            return ErrorType.BUSINESS_RULE;
        }
    }

    record InvalidStatusTransition(AccountId accountId, AccountStatus from, AccountStatus to)
            implements AccountError {
        @Override
        public String code() {
            return "ACCOUNT_INVALID_STATUS_TRANSITION";
        }

        @Override
        public String message() {
            return "Account %s cannot move from %s to %s".formatted(accountId.value(), from, to);
        }

        @Override
        public ErrorType type() {
            return ErrorType.BUSINESS_RULE;
        }
    }

    record NumberAlreadyInUse(String accountNumber) implements AccountError {
        @Override
        public String code() {
            return "ACCOUNT_NUMBER_ALREADY_IN_USE";
        }

        @Override
        public String message() {
            return "Account number " + accountNumber + " is already in use";
        }

        @Override
        public ErrorType type() {
            return ErrorType.CONFLICT;
        }
    }

    record InvalidAccountNumber(String reason) implements AccountError {
        @Override
        public String code() {
            return "ACCOUNT_INVALID_NUMBER";
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

    static AccountError notFound(AccountId id) {
        return new NotFound(id);
    }

    static AccountError insufficientFunds(AccountId id, Money requested, Money available) {
        return new InsufficientFunds(id, requested, available);
    }

    static AccountError invalidAccountNumber(String reason) {
        return new InvalidAccountNumber(reason);
    }

    /** Chooses the most specific error for an account that is not transactable. */
    static AccountError notTransactable(AccountId id, AccountStatus status) {
        return switch (status) {
            case FROZEN -> new Frozen(id);
            case CLOSED -> new Closed(id);
            case ACTIVE -> new NotActive(id, status);
        };
    }
}
