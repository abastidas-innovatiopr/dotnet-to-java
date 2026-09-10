package com.innovatiopr.payments.accounts.opening.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record OpenAccountRequest(
        @NotNull(message = "Customer id is required")
        UUID customerId,

        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "Currency must be a three-letter ISO-4217 code")
        String currency) { }
