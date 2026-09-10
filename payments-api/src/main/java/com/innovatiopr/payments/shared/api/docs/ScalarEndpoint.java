package com.innovatiopr.payments.shared.api.docs;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Serves the Scalar API reference, fully offline.
 *
 * <h2>Why this is hand-mounted instead of using the Scalar starter</h2>
 * {@code com.scalar.maven:scalar-webmvc} exists and would normally be the right dependency, but it is
 * built against Spring Boot 3.5 and its entry in
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports} is annotated
 * {@code @Configuration} rather than {@code @AutoConfiguration}. Spring Boot 4 requires the latter, so the
 * class is never even evaluated — the condition report shows no match at all and {@code /scalar} simply
 * returns 404. Depending on it would look fine in the POM and silently produce no documentation UI.
 *
 * <p>So the application depends on {@code scalar-core} only, for its bundled asset, and mounts the UI
 * itself. Two further benefits: the docs page is a {@code RouterFunction} like every other endpoint here,
 * and the ~3.7 MB {@code scalar.js} is served from the jar rather than from a CDN — which matters,
 * because this application is required to run with no external network access at all.
 */
@Component
class ScalarEndpoint {

    private static final String BUNDLE = "META-INF/resources/webjars/scalar/scalar.js";

    private final String page;

    ScalarEndpoint() {
        this.page = renderPage();
    }

    ServerResponse index(ServerRequest request) {
        return ServerResponse.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(page);
    }

    ServerResponse bundle(ServerRequest request) {
        Resource resource = new ClassPathResource(BUNDLE);
        if (!resource.exists()) {
            return ServerResponse.notFound().build();
        }
        return ServerResponse.ok()
                .contentType(MediaType.valueOf("application/javascript"))
                // The bundle is immutable for a given build, so let the browser keep it.
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic())
                .body(resource);
    }

    private static String renderPage() {
        return """
                <!doctype html>
                <html>
                  <head>
                    <title>Payments API Reference</title>
                    <meta charset="utf-8" />
                    <meta content="width=device-width, initial-scale=1" name="viewport" />
                  </head>
                  <body>
                    <div id="app"></div>
                    <script src="/docs/scalar.js"></script>
                    <script>
                      Scalar.createApiReference('#app', {
                        url: '/v3/api-docs',
                        theme: 'purple',
                        hideDownloadButton: false,
                        defaultHttpClient: { targetKey: 'shell', clientKey: 'curl' }
                      })
                    </script>
                  </body>
                </html>
                """;
    }
}
