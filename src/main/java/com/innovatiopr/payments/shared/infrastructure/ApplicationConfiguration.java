package com.innovatiopr.payments.shared.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Cross-cutting infrastructure beans.
 *
 * <p>The {@code Clock} is injected everywhere rather than calling {@code Instant.now()} inside a domain
 * method. Aggregates receive the current time as a parameter, which keeps them free of hidden ambient
 * state and lets a test pin "now" to an exact instant and assert on timestamps — the same reason a .NET
 * codebase injects {@code TimeProvider} instead of reading {@code DateTime.UtcNow}.
 */
@Configuration(proxyBeanMethods = false)
class ApplicationConfiguration {

    @Bean
    Clock clock() {
        // UTC throughout. Timestamps are stored as TIMESTAMPTZ and rendered as ISO-8601; local time
        // enters the picture only when something is displayed to a human, which this API never does.
        return Clock.systemUTC();
    }
}
