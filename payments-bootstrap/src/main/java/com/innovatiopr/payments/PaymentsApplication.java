package com.innovatiopr.payments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Application entry point.
 *
 * <p>{@code @Modulithic} declares the modular monolith and names {@code shared} as a shared module, so
 * every business module may use the kernel without listing it as a dependency.
 *
 * <p>{@code @EnableAsync} is required by {@code @ApplicationModuleListener}, which is asynchronous:
 * without it the annotation's {@code @Async} would be inert and listeners would run inline on the
 * publishing thread.
 */
@SpringBootApplication
@Modulithic(systemName = "Payments API", sharedModules = "shared")
@EnableAsync
public class PaymentsApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentsApplication.class, args);
    }
}
