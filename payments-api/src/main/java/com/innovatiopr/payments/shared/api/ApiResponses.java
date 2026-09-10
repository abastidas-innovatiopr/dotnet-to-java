package com.innovatiopr.payments.shared.api;

import com.innovatiopr.payments.shared.domain.DomainError;
import com.innovatiopr.payments.shared.domain.Result;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;
import java.util.List;
import java.util.function.Function;

/**
 * Turns a {@link Result} into a {@link ServerResponse}.
 *
 * <p>The single place where a failed result becomes an HTTP status. Endpoints call one of these methods
 * and never write {@code if (result.isFailure()) return ServerResponse.status(...)} by hand, so the
 * mapping cannot drift between slices.
 */
public final class ApiResponses {

    private ApiResponses() {
    }

    public static <T> ServerResponse ok(Result<T> result, Function<T, Object> toResource) {
        return result.fold(
                value -> ServerResponse.ok()
                        .contentType(MediaTypes.HAL_JSON)
                        .body(toResource.apply(value)),
                ApiResponses::problem);
    }

    public static <T> ServerResponse created(Result<T> result, Function<T, Object> toResource,
                                             Function<T, URI> location) {
        return result.fold(
                value -> ServerResponse.created(location.apply(value))
                        .contentType(MediaTypes.HAL_JSON)
                        .body(toResource.apply(value)),
                ApiResponses::problem);
    }

    /**
     * 201 for a transfer that moved money, 200 for one replayed from an idempotency record.
     *
     * <p>Replaying 201 would be a lie — nothing was created — while 200 with the original body tells an
     * honest story: "this is the outcome, and it already happened".
     */
    public static <T> ServerResponse createdOrReplayed(Result<T> result, Function<T, Object> toResource,
                                                       Function<T, URI> location,
                                                       Function<T, Boolean> wasReplayed) {
        return result.fold(
                value -> wasReplayed.apply(value)
                        ? ServerResponse.ok()
                                .location(location.apply(value))
                                .contentType(MediaTypes.HAL_JSON)
                                .body(toResource.apply(value))
                        : ServerResponse.created(location.apply(value))
                                .contentType(MediaTypes.HAL_JSON)
                                .body(toResource.apply(value)),
                ApiResponses::problem);
    }

    public static ServerResponse problem(List<DomainError> errors) {
        ProblemDetail detail = ProblemDetails.from(errors);
        return ServerResponse.status(detail.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(detail);
    }

    public static ServerResponse problem(HttpStatus status, String code, String message) {
        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(ProblemDetails.of(status, code, message));
    }

    /** A path variable that is not a UUID is a client mistake, not a server error. */
    public static ServerResponse invalidIdentifier(String name, String value) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_IDENTIFIER",
                "'%s' is not a valid %s".formatted(value, name));
    }
}
