package com.innovatiopr.payments.shared.api;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds RFC 9457 Problem Details.
 *
 * <h2>The one place HTTP meets the domain</h2>
 * No domain exception knows its status code. {@code AccountErrors.insufficientFunds(...)} produces a
 * {@code DomainException} and stops there; the decision that a refused business rule is
 * {@code 422 Unprocessable Entity} is an HTTP decision, and it lives in {@link ApiExceptionHandler}
 * beside every other status choice. A second transport — a gRPC service, a message consumer, a CLI —
 * would map the same exceptions differently without touching the domain.
 *
 * <h2>Why the exception type carries the status</h2>
 * The four domain exception types <em>are</em> the classification, so there is no second enum that can
 * disagree with the class it sits on:
 * <ul>
 *   <li>{@code ValidationException → 400} — the request is malformed; the client must change its shape.</li>
 *   <li>{@code NotFoundException → 404}</li>
 *   <li>{@code ConflictException → 409} — the request clashes with existing state (a reused idempotency
 *       key, a duplicate email).</li>
 *   <li>{@code DomainException → 422} — the request was well formed and understood, but a domain
 *       invariant forbids it. Insufficient funds is the canonical case: nothing is wrong with the JSON,
 *       and telling the client to fix its request would be a lie.</li>
 * </ul>
 *
 * <p>Every problem carries a stable machine-readable {@code code} extension. The {@code errors} array is
 * reserved for failures that genuinely have several parts — a request body that broke more than one
 * Bean Validation constraint — rather than being a one-element list around the {@code code} above it.
 */
public final class ProblemDetails {

    private static final String PROBLEM_BASE = "https://api.payments.local/problems/";

    private ProblemDetails() {
    }

    public static ProblemDetail validation(String detail, List<Map<String, String>> fieldErrors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Validation failed");
        problem.setType(URI.create(PROBLEM_BASE + "validation-failed"));
        problem.setProperty("code", "REQUEST_VALIDATION_FAILED");
        problem.setProperty("errors", fieldErrors);
        attachCorrelation(problem);
        return problem;
    }

    public static ProblemDetail of(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(titleFor(status));
        problem.setType(URI.create(PROBLEM_BASE + code.toLowerCase(Locale.ROOT)));
        problem.setProperty("code", code);
        attachCorrelation(problem);
        return problem;
    }

    /** Echoes the correlation id so a client can quote it and an operator can find the request in the logs. */
    private static void attachCorrelation(ProblemDetail problem) {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID);
        if (correlationId != null) {
            problem.setProperty("correlationId", correlationId);
        }
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            problem.setProperty("traceId", traceId);
        }
    }

    private static String titleFor(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Invalid request";
            case FORBIDDEN -> "Forbidden";
            case NOT_FOUND -> "Resource not found";
            case CONFLICT -> "Conflict";
            case UNPROCESSABLE_ENTITY -> "Business rule violated";
            case INTERNAL_SERVER_ERROR -> "Internal server error";
            case GATEWAY_TIMEOUT -> "Upstream timeout";
            default -> status.getReasonPhrase();
        };
    }
}
