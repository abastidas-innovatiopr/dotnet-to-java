package com.innovatiopr.payments.payments.transfer.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * HTTP request body for a transfer.
 *
 * <p>{@code @Digits(integer = 15, fraction = 4)} mirrors the {@code NUMERIC(19, 4)} column, so an
 * over-precise amount is rejected with a clear 400 rather than being silently rounded on the way in.
 *
 * <p>The idempotency key is <em>not</em> in the body — it travels in the {@code Idempotency-Key} header,
 * because it describes the request rather than the transfer, and because it must be identical across
 * retries whose bodies are byte-for-byte the same.
 */
public record TransferRequest(
        @NotNull(message = "Source account id is required")
        UUID sourceAccountId,

        @NotNull(message = "Destination account id is required")
        UUID destinationAccountId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.0001", message = "Amount must be greater than zero")
        @Digits(integer = 15, fraction = 4, message = "Amount must have at most 15 integer and 4 fractional digits")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "Currency must be a three-letter ISO-4217 code")
        String currency,

        @Size(max = 140, message = "Reference must be at most 140 characters")
        String reference) {

    /**
     * Canonical form used for the idempotency request hash.
     *
     * <p>Built from the parsed fields rather than the raw bytes so that formatting differences — key
     * order, whitespace, {@code 100} versus {@code 100.00} — do not make an identical retry look like a
     * different request. Two requests that mean the same thing must hash the same.
     */
    public String canonicalForm() {
        return String.join("|",
                String.valueOf(sourceAccountId),
                String.valueOf(destinationAccountId),
                amount == null ? "" : amount.stripTrailingZeros().toPlainString(),
                currency == null ? "" : currency.toUpperCase(java.util.Locale.ROOT),
                reference == null ? "" : reference.trim());
    }
}
