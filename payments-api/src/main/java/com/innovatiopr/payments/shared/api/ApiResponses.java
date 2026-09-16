package com.innovatiopr.payments.shared.api;

import org.springframework.http.ResponseEntity;

import java.net.URI;

/**
 * The two write responses whose status is not a constant.
 *
 * <p>Everything else is expressed directly on the controller method: a {@code GET} returns its resource
 * and Spring sends 200, and a failure leaves as an exception for {@link ApiExceptionHandler}. What cannot
 * be written as an annotation is a status chosen at runtime — {@code @ResponseStatus} takes a compile-time
 * constant — which is exactly what the idempotent transfer below needs.
 */
public final class ApiResponses {

    private ApiResponses() {
    }

    public static <T> ResponseEntity<T> created(T body, URI location) {
        return ResponseEntity.created(location).body(body);
    }

    /**
     * 201 for a transfer that moved money, 200 for one replayed from an idempotency record.
     *
     * <p>Replaying 201 would be a lie — nothing was created — while 200 with the original body tells an
     * honest story: "this is the outcome, and it already happened". The {@code Location} header is sent
     * either way, because the transaction it points at exists in both cases.
     */
    public static <T> ResponseEntity<T> createdOrReplayed(T body, URI location, boolean replayed) {
        return replayed
                ? ResponseEntity.ok().location(location).body(body)
                : ResponseEntity.created(location).body(body);
    }
}
