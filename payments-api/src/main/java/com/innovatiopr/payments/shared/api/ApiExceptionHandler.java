package com.innovatiopr.payments.shared.api;

import com.innovatiopr.payments.shared.domain.ConflictException;
import com.innovatiopr.payments.shared.domain.CurrencyMismatchException;
import com.innovatiopr.payments.shared.domain.DomainException;
import com.innovatiopr.payments.shared.domain.ForbiddenException;
import com.innovatiopr.payments.shared.domain.NotFoundException;
import com.innovatiopr.payments.shared.domain.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * The one place HTTP meets the domain: every exception becomes an RFC 9457 problem document here.
 *
 * <h2>Three kinds of failure, one table</h2>
 * <ol>
 *   <li><b>Expected business outcomes</b> — {@code ValidationException}, {@code NotFoundException},
 *       {@code ConflictException}, {@code DomainException}. Thrown by aggregates and handlers, mapped by
 *       type to 400 / 404 / 409 / 422, and reported with the stable {@code code} the domain attached.</li>
 *   <li><b>Transport failures</b> — unreadable JSON, a path variable that is not a UUID, an unmapped
 *       path, a body that broke Bean Validation. These fail before any handler runs.</li>
 *   <li><b>Bugs and infrastructure faults</b> — everything else, reported as a deliberately vague 500
 *       with the detail sent to the log rather than to the client.</li>
 * </ol>
 *
 * <p>Ordering in this file is documentation, not semantics: Spring dispatches to the handler whose
 * parameter type is closest to the thrown exception, so the catch-all at the bottom only runs when
 * nothing above matches.
 *
 * <p>Deliberately <em>not</em> extending {@code ResponseEntityExceptionHandler}: that base class
 * substitutes Spring's own problem bodies for the framework exceptions, which would silently drop the
 * {@code code}, {@code errors} and {@code correlationId} extensions that {@link ProblemDetails} adds.
 */
@RestControllerAdvice
class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private static final String UNIQUE_VIOLATION = "23505";
    private static final String FOREIGN_KEY_VIOLATION = "23503";

    /** A domain invariant refused a well-formed request. */
    @ExceptionHandler(DomainException.class)
    ResponseEntity<ProblemDetail> handleDomainRule(DomainException e) {
        log.debug("Domain rule refused the request: {} {}", e.code(), e.getMessage());
        return problem(ProblemDetails.of(HttpStatus.UNPROCESSABLE_ENTITY, e.code(), e.getMessage()));
    }

    /** The domain refused a value outright — malformed, out of range, structurally invalid. */
    @ExceptionHandler(ValidationException.class)
    ResponseEntity<ProblemDetail> handleDomainValidation(ValidationException e) {
        log.debug("Rejected an invalid value: {} {}", e.code(), e.getMessage());
        return problem(ProblemDetails.of(HttpStatus.BAD_REQUEST, e.code(), e.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ProblemDetail> handleNotFound(NotFoundException e) {
        return problem(ProblemDetails.of(HttpStatus.NOT_FOUND, e.code(), e.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ProblemDetail> handleConflict(ConflictException e) {
        log.info("Conflict: {} {}", e.code(), e.getMessage());
        return problem(ProblemDetails.of(HttpStatus.CONFLICT, e.code(), e.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    ResponseEntity<ProblemDetail> handleForbidden(ForbiddenException e) {
        return problem(ProblemDetails.of(HttpStatus.FORBIDDEN, e.code(), e.getMessage()));
    }

    /**
     * A request body that broke one or more Bean Validation constraints.
     *
     * <p>This is the failure that genuinely has several parts, so it is the one that fills the
     * {@code errors} array. Sorted by field name to keep the response stable between requests.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleInvalidBody(MethodArgumentNotValidException e) {
        List<Map<String, String>> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage() == null ? "is invalid" : error.getDefaultMessage()))
                .sorted(Comparator.comparing(entry -> entry.get("field")))
                .toList();
        return problem(ProblemDetails.validation("The request body failed validation", fieldErrors));
    }

    /** Bean Validation on a method parameter — a query or path value rather than the body. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ProblemDetail> handleInvalidParameter(HandlerMethodValidationException e) {
        return problem(ProblemDetails.of(HttpStatus.BAD_REQUEST, "REQUEST_VALIDATION_FAILED",
                "One or more request parameters are invalid"));
    }

    /**
     * A path variable that will not convert — {@code /accounts/not-a-uuid}.
     *
     * <p>With {@code @PathVariable UUID} the conversion happens before the controller method is entered,
     * so this replaces the hand-written {@code try}/{@code catch} each endpoint used to carry.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ProblemDetail> handleBadPathVariable(MethodArgumentTypeMismatchException e) {
        return problem(ProblemDetails.of(HttpStatus.BAD_REQUEST, "INVALID_IDENTIFIER",
                "'%s' is not a valid %s".formatted(e.getValue(), e.getName())));
    }

    /** Unparseable JSON, a malformed number, a body that is not JSON at all. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> handleUnreadableBody(HttpMessageNotReadableException e) {
        log.debug("Rejected an unreadable request body: {}", e.getMessage());
        return problem(ProblemDetails.of(HttpStatus.BAD_REQUEST, "REQUEST_BODY_UNREADABLE",
                "The request body could not be parsed as JSON"));
    }

    /**
     * A concurrent update won the {@code @Version} check.
     *
     * <p>409 rather than 500: nothing is broken, the client simply lost a race and retrying will usually
     * succeed. Note that transfers do not rely on this path — they take pessimistic row locks, so they
     * queue rather than collide.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> handleOptimisticLock(OptimisticLockingFailureException e) {
        log.info("Optimistic lock conflict: {}", e.getMessage());
        return problem(ProblemDetails.of(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "The resource was modified concurrently; retry the request"));
    }

    /**
     * A unique or foreign-key constraint rejected the write.
     *
     * <p>The SQLState is read through the JDBC-standard {@link SQLException#getSQLState()} rather than by
     * unwrapping to a PostgreSQL exception type, so the API module needs no database-driver dependency.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> handleIntegrityViolation(DataIntegrityViolationException e) {
        String sqlState = sqlStateOf(e);
        if (UNIQUE_VIOLATION.equals(sqlState)) {
            log.info("Unique constraint rejected the write: {}", e.getMessage());
            return problem(ProblemDetails.of(HttpStatus.CONFLICT, "RESOURCE_ALREADY_EXISTS",
                    "A resource with the same unique identifier already exists"));
        }
        if (FOREIGN_KEY_VIOLATION.equals(sqlState)) {
            return problem(ProblemDetails.of(HttpStatus.BAD_REQUEST, "INVALID_REFERENCE",
                    "The request references a resource that does not exist"));
        }
        log.error("Unclassified data integrity violation", e);
        return problem(ProblemDetails.of(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "The request could not be completed"));
    }

    /** The database took too long. 504 rather than 500: the request may well succeed if retried. */
    @ExceptionHandler(QueryTimeoutException.class)
    ResponseEntity<ProblemDetail> handleTimeout(QueryTimeoutException e) {
        log.warn("Query timed out", e);
        return problem(ProblemDetails.of(HttpStatus.GATEWAY_TIMEOUT, "UPSTREAM_TIMEOUT",
                "The request timed out; retry it"));
    }

    /**
     * Money arithmetic across currencies. Reaching this handler means a caller skipped the currency check
     * that every aggregate performs — a bug, so it is a 500 and it is logged at error level.
     */
    @ExceptionHandler(CurrencyMismatchException.class)
    ResponseEntity<ProblemDetail> handleCurrencyMismatch(CurrencyMismatchException e) {
        log.error("Currency mismatch escaped domain validation", e);
        return problem(ProblemDetails.of(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "The request could not be completed"));
    }

    /**
     * An unmapped path.
     *
     * <p>Without this, {@code NoResourceFoundException} falls through to the catch-all below and every
     * typo in a URL is reported as a 500 — which is both wrong and alarming on a dashboard. Functional
     * this is easy to miss: an unmatched request is handed to the static-resource handler, and it is that
     * handler, not the dispatcher, that raises the exception.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ProblemDetail> handleNoResource(NoResourceFoundException e) {
        return problem(ProblemDetails.of(HttpStatus.NOT_FOUND, "ENDPOINT_NOT_FOUND",
                "No endpoint is mapped to " + e.getResourcePath()));
    }

    /** An unsupported HTTP method on a path that does exist. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ProblemDetail> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return problem(ProblemDetails.of(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                "%s is not supported on this endpoint".formatted(e.getMethod())));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception e) {
        // The message is deliberately generic: an exception message can leak schema details, and this is
        // a financial API. The detail goes to the log, correlated by id, not to the client.
        log.error("Unhandled exception while serving a request", e);
        return problem(ProblemDetails.of(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "The request could not be completed"));
    }

    /** Walks the cause chain for the first {@link SQLException} and returns its SQLState, if any. */
    private static String sqlStateOf(Throwable throwable) {
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException) {
                return sqlException.getSQLState();
            }
        }
        return null;
    }

    private ResponseEntity<ProblemDetail> problem(ProblemDetail detail) {
        return ResponseEntity.status(detail.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(detail);
    }
}
