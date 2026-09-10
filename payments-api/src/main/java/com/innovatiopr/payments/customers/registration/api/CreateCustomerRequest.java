package com.innovatiopr.payments.customers.registration.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * HTTP request body for customer registration.
 *
 * <p>The Bean Validation annotations here are <em>transport</em> validation — they reject a body that is
 * structurally unusable, before any domain type is constructed. They do not replace
 * {@code EmailAddress.create}: a request could arrive from a test, a seeder or a future message consumer
 * that never passes through this record, and the domain must still refuse a malformed address.
 */
public record CreateCustomerRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must be at most 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must be at most 100 characters")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        @Size(max = 254, message = "Email must be at most 254 characters")
        String email) { }
