package com.innovatiopr.payments.shared.application;

/**
 * Marker for a use case that reads state and changes nothing.
 *
 * <p>Queries are answered from read models built with {@code JdbcClient}, never by loading write-side
 * aggregates. That is the whole point of CQRS here: the two sides can evolve independently.
 */
public interface Query { }
