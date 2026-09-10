package com.innovatiopr.payments.payments.deposits.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Shared request body for deposits and withdrawals. */
public record CashMovementRequest(
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.0001", message = "Amount must be greater than zero")
        @Digits(integer = 15, fraction = 4, message = "Amount must have at most 15 integer and 4 fractional digits")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "Currency must be a three-letter ISO-4217 code")
        String currency,

        @Size(max = 140, message = "Reference must be at most 140 characters")
        String reference) { }
