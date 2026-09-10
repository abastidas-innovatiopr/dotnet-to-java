package com.innovatiopr.payments.shared.api;

import com.innovatiopr.payments.shared.domain.CurrencyMismatchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Last-resort translation of exceptions into Problem Details.
 *
 * <h2>This complements {@code Result}, it does not replace it</h2>
 * Expected business outcomes never reach here — they travel as {@code Result} failures and are mapped by
 * {@link ApiResponses}. What lands here is the other category: bugs, infrastructure failures, and
 * malformed input that failed before any handler ran. That separation is the whole reason for having a
 * {@code Result} type at all.
 *
 * <p>{@code @RestControllerAdvice} works with functional endpoints: an exception thrown from a
 * {@code HandlerFunction} propagates to the {@code DispatcherServlet}, which consults the same
 * {@code HandlerExceptionResolver} chain it uses for annotated controllers.
 */
@RestControllerAdvice
class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

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
     * routing makes this easy to miss: an unmatched {@code RouterFunction} hands the request to the
     * static-resource handler, and it is that handler, not the router, that raises the exception.
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

    private ResponseEntity<ProblemDetail> problem(ProblemDetail detail) {
        return ResponseEntity.status(detail.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(detail);
    }
}
