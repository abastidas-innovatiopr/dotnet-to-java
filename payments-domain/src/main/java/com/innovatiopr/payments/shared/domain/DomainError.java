package com.innovatiopr.payments.shared.domain;

/**
 * A first-class, expected business failure.
 *
 * <h2>Why this interface is not {@code sealed}</h2>
 * The original design called for:
 * <pre>{@code
 * sealed interface DomainError permits AccountError, MoneyError, TransferError, ... { }
 * }</pre>
 * That does not compile here. The Java Language Specification (JLS 8.1.1.2 / 9.1.1.4) requires every
 * permitted subtype of a sealed type to live in the <em>same package</em>, unless the whole hierarchy
 * lives in one <em>named JPMS module</em>. Our errors deliberately live in their own bounded contexts
 * ({@code accounts.domain.AccountError}, {@code payments.domain.TransferError}, ...), so a sealed root
 * would either fail to compile or force every module's errors back into one shared package — which
 * would destroy the module boundaries this application exists to demonstrate.
 *
 * <p>The exhaustiveness that sealing buys is therefore applied one level down: each module publishes a
 * {@code sealed interface} of its own (for example {@code AccountError}) whose permitted implementations
 * are nested records in the same file. Inside a module you still get exhaustive {@code switch}; across
 * modules you program against {@code code()} and {@link ErrorType}, which is what the HTTP layer needs
 * anyway.
 */
public interface DomainError {

    /**
     * Stable, machine-readable identifier such as {@code ACCOUNT_INSUFFICIENT_FUNDS}.
     * Clients may branch on this value, so it is part of the public contract and must not change.
     */
    String code();

    /** Human-readable explanation. Safe to show to an operator; never contains secrets. */
    String message();

    /** Domain-level classification used by outer layers to choose a transport representation. */
    ErrorType type();
}
