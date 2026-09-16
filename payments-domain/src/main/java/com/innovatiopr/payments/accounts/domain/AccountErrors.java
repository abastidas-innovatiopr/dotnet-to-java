package com.innovatiopr.payments.accounts.domain;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.shared.domain.ConflictException;
import com.innovatiopr.payments.shared.domain.DomainException;
import com.innovatiopr.payments.shared.domain.Money;
import com.innovatiopr.payments.shared.domain.NotFoundException;
import com.innovatiopr.payments.shared.domain.ValidationException;

/**
 * Expected business failures owned by the Accounts bounded context.
 *
 * <p>Every factory pairs a stable {@code code} with its message in one place, so a code can never drift
 * from the sentence that explains it. The exception <em>type</em> carries the transport meaning: the API
 * layer renders a {@link NotFoundException} as 404 and a {@link DomainException} as 422 without ever
 * asking the domain what status it wants. Nothing here mentions HTTP.
 */
public final class AccountErrors {

    private AccountErrors() {
    }

    public static NotFoundException notFound(AccountId id) {
        return new NotFoundException("ACCOUNT_NOT_FOUND", "No account exists with id " + id.value());
    }

    public static DomainException insufficientFunds(AccountId id, Money requested, Money available) {
        return new DomainException(
                "ACCOUNT_INSUFFICIENT_FUNDS",
                "Account %s has %s available but %s was requested".formatted(id.value(), available, requested));
    }

    public static ValidationException invalidAccountNumber(String reason) {
        return new ValidationException("ACCOUNT_INVALID_NUMBER", reason);
    }

    public static ConflictException numberAlreadyInUse(String accountNumber) {
        return new ConflictException(
                "ACCOUNT_NUMBER_ALREADY_IN_USE",
                "Account number " + accountNumber + " is already in use");
    }

    public static DomainException notEmpty(AccountId id, Money balance) {
        return new DomainException(
                "ACCOUNT_NOT_EMPTY",
                "Account %s cannot be closed while it holds %s".formatted(id.value(), balance));
    }

    public static DomainException invalidStatusTransition(AccountId id, AccountStatus from, AccountStatus to) {
        return new DomainException(
                "ACCOUNT_INVALID_STATUS_TRANSITION",
                "Account %s cannot move from %s to %s".formatted(id.value(), from, to));
    }

    /**
     * Chooses the most specific error for an account that is not transactable. The {@code switch} stays
     * exhaustive over {@link AccountStatus}, so adding a status is a compile error here rather than a
     * silently generic message.
     */
    public static DomainException notTransactable(AccountId id, AccountStatus status) {
        return switch (status) {
            case FROZEN -> new DomainException(
                    "ACCOUNT_FROZEN",
                    "Account %s is frozen and can neither send nor receive money".formatted(id.value()));
            case CLOSED -> new DomainException(
                    "ACCOUNT_CLOSED",
                    "Account %s is closed".formatted(id.value()));
            case ACTIVE -> new DomainException(
                    "ACCOUNT_NOT_ACTIVE",
                    "Account %s is %s and cannot transact".formatted(id.value(), status));
        };
    }
}
