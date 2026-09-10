package com.innovatiopr.payments.accounts.details.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountBalance(UUID accountId, String currency, BigDecimal balance, String status, Instant asOf) { }
