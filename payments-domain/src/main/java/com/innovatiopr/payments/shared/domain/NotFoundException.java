package com.innovatiopr.payments.shared.domain;

import java.util.Objects;

/**
 * A referenced aggregate or entity does not exist. Surfaces as HTTP 404.
 *
 * <h2>Why this lives in the domain module</h2>
 * Its .NET counterpart sits in the application layer, because that is where the repositories are
 * consumed. Here it must live one layer further in, because the published module contracts
 * ({@code AccountsApi}, {@code CustomersApi}, {@code LedgerApi}, {@code PaymentsApi}) are declared in
 * {@code payments-domain} and name this type in their {@code @throws} clauses. A type in the
 * application module would be invisible to them, and the dependency arrow points the wrong way.
 *
 * <p>It stays plain Java — no Spring, no Jakarta — so the domain-purity rules in {@code ArchitectureTest}
 * hold unchanged.
 */
public final class NotFoundException extends RuntimeException {

    private final String code;

    public NotFoundException(String code, String message) {
        super(Objects.requireNonNull(message, "message"));
        this.code = Objects.requireNonNull(code, "code");
    }

    /**
     * Names the missing thing without the caller having to phrase the message. Mirrors the reference's
     * {@code NotFoundException.For(nameof(Company), id)}; the code is derived as
     * {@code ACCOUNT_NOT_FOUND} from a name of {@code Account}.
     */
    public static NotFoundException of(String entityName, Object key) {
        Objects.requireNonNull(entityName, "entityName");
        return new NotFoundException(
                entityName.toUpperCase(java.util.Locale.ROOT) + "_NOT_FOUND",
                "%s '%s' was not found".formatted(entityName, key));
    }

    public String code() {
        return code;
    }
}
