package com.innovatiopr.payments.customers.domain;

import com.innovatiopr.payments.shared.domain.Result;

import java.util.Objects;

public record PersonName(String firstName, String lastName) {

    private static final int MAX_PART_LENGTH = 100;

    public PersonName {
        Objects.requireNonNull(firstName, "firstName");
        Objects.requireNonNull(lastName, "lastName");
    }

    public static Result<PersonName> create(String firstName, String lastName) {
        if (firstName == null || firstName.isBlank()) {
            return Result.failure(CustomerError.invalidName("First name is required"));
        }
        if (lastName == null || lastName.isBlank()) {
            return Result.failure(CustomerError.invalidName("Last name is required"));
        }
        String first = firstName.trim();
        String last = lastName.trim();
        if (first.length() > MAX_PART_LENGTH || last.length() > MAX_PART_LENGTH) {
            return Result.failure(CustomerError.invalidName("Name parts must be at most " + MAX_PART_LENGTH + " characters"));
        }
        return Result.success(new PersonName(first, last));
    }

    public static PersonName fromStorage(String firstName, String lastName) {
        return new PersonName(firstName, lastName);
    }

    public String fullName() {
        return firstName + " " + lastName;
    }
}
