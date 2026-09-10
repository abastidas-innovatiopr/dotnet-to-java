package com.innovatiopr.payments.shared.application;

import com.innovatiopr.payments.shared.domain.Result;

/**
 * Handles exactly one {@link Query}.
 *
 * @param <Q> the query type
 * @param <R> the payload produced on success
 */
public interface QueryHandler<Q extends Query, R> {

    Result<R> handle(Q query);
}
