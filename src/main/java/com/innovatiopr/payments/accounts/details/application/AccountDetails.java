package com.innovatiopr.payments.accounts.details.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountDetails(UUID id, UUID customerId, String accountNumber, String currency,
                             BigDecimal balance, String status, Instant openedAt) { }
