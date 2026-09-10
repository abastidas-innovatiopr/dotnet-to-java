package com.innovatiopr.payments.accounts.opening.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OpenAccountResult(UUID accountId, UUID customerId, String accountNumber, String currency,
                                BigDecimal balance, String status, Instant openedAt) { }
