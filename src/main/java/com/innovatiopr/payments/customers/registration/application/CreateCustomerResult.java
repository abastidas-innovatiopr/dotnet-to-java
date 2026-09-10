package com.innovatiopr.payments.customers.registration.application;

import java.time.Instant;
import java.util.UUID;

public record CreateCustomerResult(UUID customerId, String firstName, String lastName, String email,
                                   Instant registeredAt) { }
