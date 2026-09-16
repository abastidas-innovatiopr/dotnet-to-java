package com.innovatiopr.payments.shared.application;

/**
 * Handles exactly one {@link Query}.
 *
 * @param <Q> the query type
 * @param <R> the payload produced on success
 */
public interface QueryHandler<Q extends Query, R> {

    R handle(Q query);
}
