package com.innovatiopr.payments.shared.api;

import com.innovatiopr.payments.shared.domain.DomainError;
import com.innovatiopr.payments.shared.domain.ErrorType;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Translates domain errors into RFC 9457 Problem Details.
 *
 * <h2>The one place HTTP meets the domain</h2>
 * No {@code DomainError} knows its status code. {@code AccountError.InsufficientFunds} classifies itself
 * as a {@link ErrorType#BUSINESS_RULE} and stops there; the decision that a business-rule violation is
 * {@code 422 Unprocessable Entity} is an HTTP decision and lives here. A second transport — a gRPC
 * service, a message consumer, a CLI — would map the same errors differently without touching the domain.
 *
 * <h2>Status choices</h2>
 * <ul>
 *   <li>{@code VALIDATION → 400} — the request is malformed; the client must change its shape.</li>
 *   <li>{@code NOT_FOUND → 404}</li>
 *   <li>{@code CONFLICT → 409} — the request clashes with existing state (a reused idempotency key, a
 *       duplicate email).</li>
 *   <li>{@code BUSINESS_RULE → 422} — the request was well formed and understood, but a domain invariant
 *       forbids it. Insufficient funds is the canonical case: nothing is wrong with the JSON.</li>
 * </ul>
 *
 * <p>When several errors are present the response takes the most severe status, and every error is listed
 * in the {@code errors} extension so a client can act on each machine-readable {@code code}.
 */
public final class ProblemDetails {

    private static final String PROBLEM_BASE = "https://api.payments.local/problems/";

    private ProblemDetails() {
    }

    public static ProblemDetail from(List<DomainError> errors) {
        DomainError primary = errors.getFirst();
        HttpStatus status = statusFor(primary.type());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, primary.message());
        problem.setTitle(titleFor(status));
        problem.setType(URI.create(PROBLEM_BASE + primary.code().toLowerCase(java.util.Locale.ROOT)));
        problem.setProperty("code", primary.code());
        problem.setProperty("errors", errors.stream()
                .map(error -> Map.of(
                        "code", error.code(),
                        "message", error.message(),
                        "type", error.type().name()))
                .toList());
        attachCorrelation(problem);
        return problem;
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
        problem.setType(URI.create(PROBLEM_BASE + code.toLowerCase(java.util.Locale.ROOT)));
        problem.setProperty("code", code);
        attachCorrelation(problem);
        return problem;
    }

    public static HttpStatus statusFor(ErrorType type) {
        return switch (type) {
            case VALIDATION -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case BUSINESS_RULE -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
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
            case NOT_FOUND -> "Resource not found";
            case CONFLICT -> "Conflict";
            case UNPROCESSABLE_ENTITY -> "Business rule violated";
            default -> status.getReasonPhrase();
        };
    }
}
