package com.innovatiopr.payments.shared.api.docs;

/**
 * Shared OpenAPI text: tag names, and the prose that would otherwise be retyped in several slices.
 *
 * <p>These are {@code String} constants rather than the builder fragments this class used to hold,
 * because annotation attributes must be compile-time constants — {@code @Parameter(description = ...)}
 * cannot call a method. The mechanical half of what the builders did (attaching
 * {@code application/hal+json} to success responses and {@code application/problem+json} to failures) has
 * moved to {@link PaymentsOperationCustomizer}, which applies it to every operation at once instead of
 * asking each one to remember.
 *
 * <p>That split is an improvement on what it replaced: the prose stays at the operation it describes, and
 * the plumbing is declared once and cannot be forgotten.
 */
public final class OpenApiDocs {

    public static final String TAG_CUSTOMERS = "Customers";
    public static final String TAG_ACCOUNTS = "Accounts";
    public static final String TAG_PAYMENTS = "Payments";
    public static final String TAG_TRANSACTIONS = "Transactions";
    public static final String TAG_LEDGER = "Ledger";

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    public static final String IDEMPOTENCY_KEY_DESCRIPTION =
            "Client-generated key that makes this request safe to retry. Resend the SAME key after a "
                    + "timeout: the server guarantees the money moves at most once. Replaying the key with "
                    + "an identical body returns the original result with 200; replaying it with a "
                    + "different body returns 409. 8-255 characters of letters, digits, '.', '_', ':' or "
                    + "'-' - a UUID is a good choice.";

    public static final String IDEMPOTENCY_KEY_EXAMPLE = "6a1f4c2e-9d3b-4f1a-8c7e-2b5d0a9f3e11";

    public static final String ACCOUNT_ID_DESCRIPTION = "Account identifier.";
    public static final String CUSTOMER_ID_DESCRIPTION = "Customer identifier.";
    public static final String TRANSACTION_ID_DESCRIPTION = "Transaction identifier.";

    public static final String VALIDATION_FAILED_DESCRIPTION =
            "The request body or a path variable failed transport validation. The `errors` array names "
                    + "each offending field.";

    private OpenApiDocs() {
    }
}
