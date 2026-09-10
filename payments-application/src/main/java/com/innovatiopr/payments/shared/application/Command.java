package com.innovatiopr.payments.shared.application;

/**
 * Marker for a use case that changes state.
 *
 * <p>Commands are immutable records built by the API layer from an HTTP request. They carry domain value
 * objects wherever a meaningful domain concept exists, not loose strings and UUIDs.
 */
public interface Command { }
