package com.innovatiopr.payments.accounts.status.application;

import java.util.UUID;

public record AccountStatusResult(UUID accountId, String status) { }
