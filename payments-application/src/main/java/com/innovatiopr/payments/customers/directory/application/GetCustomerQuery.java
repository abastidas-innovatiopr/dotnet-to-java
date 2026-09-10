package com.innovatiopr.payments.customers.directory.application;

import com.innovatiopr.payments.customers.CustomerId;
import com.innovatiopr.payments.shared.application.Query;

public record GetCustomerQuery(CustomerId customerId) implements Query { }
