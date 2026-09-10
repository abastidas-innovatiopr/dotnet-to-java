package com.innovatiopr.payments.customers.directory.application;

import com.innovatiopr.payments.shared.application.PageRequest;
import com.innovatiopr.payments.shared.application.Query;

public record ListCustomersQuery(PageRequest page) implements Query { }
