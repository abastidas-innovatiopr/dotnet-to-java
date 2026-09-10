package com.innovatiopr.payments.shared.api;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Runs Jakarta Bean Validation on a request body inside a functional endpoint.
 *
 * <h2>Why this class has to exist</h2>
 * {@code @Valid} is processed by the argument resolvers of the annotated-controller machinery. Functional
 * endpoints receive a {@code ServerRequest} and call {@code body(Class)} themselves, so nothing inspects
 * the annotations — validation simply does not happen unless it is invoked. This is the second real cost
 * of choosing {@code RouterFunction}, alongside losing {@code linkTo(methodOn(...))}, and it is easy to
 * overlook until a null slips into a handler.
 *
 * <h2>Three kinds of validation, not one</h2>
 * <ul>
 *   <li><b>Transport</b> (here): is the payload structurally usable? {@code @NotNull}, {@code @Positive},
 *       {@code @NotBlank}. Answerable without touching the database → 400.</li>
 *   <li><b>Application</b>: is the referenced thing there, is this key already used? Needs a lookup, and
 *       lives in handlers → 404 or 409.</li>
 *   <li><b>Domain invariants</b>: does this violate a rule of the business? Needs current state, and
 *       lives in aggregates → 422.</li>
 * </ul>
 * A missing {@code amount} is transport validation. Insufficient funds is a domain invariant. Collapsing
 * the two into "validation" is what produces APIs that answer 400 for a request that was perfectly well
 * formed.
 */
@Component
public class RequestValidator {

    private final Validator validator;

    public RequestValidator(Validator validator) {
        this.validator = validator;
    }

    /** @return a 400 Problem Details response when the body is invalid, otherwise empty. */
    public <T> Optional<ServerResponse> validate(T body) {
        Set<ConstraintViolation<T>> violations = validator.validate(body);
        if (violations.isEmpty()) {
            return Optional.empty();
        }

        List<Map<String, String>> fieldErrors = violations.stream()
                .map(violation -> Map.of(
                        "field", violation.getPropertyPath().toString(),
                        "message", violation.getMessage()))
                .sorted(Comparator.comparing(entry -> entry.get("field")))
                .toList();

        return Optional.of(ServerResponse.badRequest()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(ProblemDetails.validation("The request body failed validation", fieldErrors)));
    }
}
