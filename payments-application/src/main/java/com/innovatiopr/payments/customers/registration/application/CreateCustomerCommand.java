package com.innovatiopr.payments.customers.registration.application;

import com.innovatiopr.payments.shared.application.Command;

public record CreateCustomerCommand(String firstName, String lastName, String email) implements Command { }
