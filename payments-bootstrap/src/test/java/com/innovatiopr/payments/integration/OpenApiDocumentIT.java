package com.innovatiopr.payments.integration;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the generated OpenAPI document.
 *
 * <h2>Why this test exists</h2>
 * springdoc discovers annotated controllers by reflection, and a {@code RouterFunction} is an opaque
 * runtime object. With plain {@code RouterFunctions.route()} the document generated **zero paths** and
 * Scalar rendered an empty API reference — a silent failure: the application worked, the docs endpoint
 * returned 200, and the page was simply blank.
 *
 * <p>{@code SpringdocRouteBuilder} fixes it by taking documentation in the same call as the route. This
 * test makes the fix permanent: add a route with the wrong builder and the build fails rather than the
 * documentation quietly shrinking.
 */
class OpenApiDocumentIT extends AbstractIntegrationTest {

    /** Every route the application serves under /api/v1. */
    private static final List<String> EXPECTED_OPERATIONS = List.of(
            "post /api/v1/customers",
            "get /api/v1/customers",
            "get /api/v1/customers/{customerId}",
            "post /api/v1/accounts",
            "get /api/v1/accounts/{accountId}",
            "get /api/v1/accounts/{accountId}/balance",
            "post /api/v1/accounts/{accountId}/freeze",
            "post /api/v1/accounts/{accountId}/unfreeze",
            "post /api/v1/accounts/{accountId}/close",
            "post /api/v1/accounts/{accountId}/deposits",
            "post /api/v1/accounts/{accountId}/withdrawals",
            "get /api/v1/accounts/{accountId}/transactions",
            "get /api/v1/accounts/{accountId}/statement",
            "post /api/v1/transfers",
            "get /api/v1/transactions",
            "get /api/v1/transactions/{transactionId}");

    @LocalServerPort
    int port;

    private DocumentContext apiDocs;

    @BeforeEach
    void fetchDocument() {
        byte[] body = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build()
                .get().uri("/v3/api-docs").exchange()
                .expectStatus().isOk()
                .expectBody().returnResult().getResponseBody();
        apiDocs = JsonPath.using(Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS))
                .parse(new String(body, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("every functional route appears in the OpenAPI document")
    void documents_every_route() {
        Map<String, Map<String, Object>> paths = apiDocs.read("$.paths");

        List<String> documented = paths.entrySet().stream()
                .flatMap(entry -> entry.getValue().keySet().stream()
                        .map(method -> method + " " + entry.getKey()))
                .toList();

        assertThat(documented).containsExactlyInAnyOrderElementsOf(EXPECTED_OPERATIONS);
    }

    @Test
    @DisplayName("the document is a valid OpenAPI 3.1 descriptor with API-level metadata")
    void has_document_metadata() {
        assertThat((String) apiDocs.read("$.openapi")).startsWith("3.1");
        assertThat((String) apiDocs.read("$.info.title")).isEqualTo("Payments API");
        assertThat((String) apiDocs.read("$.info.version")).isEqualTo("1.0.0");
        assertThat((String) apiDocs.read("$.info.description"))
                .contains("Pagination", "Idempotency", "Hypermedia", "Problem Details");
    }

    @Test
    @DisplayName("every operation carries an id, a summary and a tag")
    void every_operation_is_described() {
        Map<String, Map<String, Map<String, Object>>> paths = apiDocs.read("$.paths");

        paths.forEach((path, methods) -> methods.forEach((method, operation) -> {
            String where = method.toUpperCase() + " " + path;
            assertThat(operation.get("operationId")).as("operationId for " + where).isNotNull();
            assertThat(operation.get("summary")).as("summary for " + where).isNotNull();
            assertThat((List<?>) operation.get("tags")).as("tags for " + where).isNotEmpty();
            assertThat((Map<?, ?>) operation.get("responses")).as("responses for " + where).isNotEmpty();
        }));
    }

    @Test
    @DisplayName("request bodies are described by real schemas")
    void request_schemas_are_generated() {
        Map<String, Object> schemas = apiDocs.read("$.components.schemas");

        assertThat(schemas.keySet()).contains(
                "TransferRequest", "CreateCustomerRequest", "OpenAccountRequest", "CashMovementRequest");

        // Bean Validation constraints reach the schema, so the docs state the same limits the API enforces.
        assertThat((String) apiDocs.read("$.components.schemas.TransferRequest.properties.sourceAccountId.format"))
                .isEqualTo("uuid");
        assertThat(apiDocs.<Object>read("$.components.schemas.TransferRequest.properties.amount.minimum"))
                .isNotNull();
        assertThat((int) apiDocs.read("$.components.schemas.TransferRequest.properties.reference.maxLength"))
                .isEqualTo(140);
    }

    @Test
    @DisplayName("the transfer operation documents its Idempotency-Key header and every status it returns")
    void transfer_is_fully_documented() {
        Map<String, Object> transfer = apiDocs.read("$.paths.['/api/v1/transfers'].post");

        List<Map<String, Object>> parameters = apiDocs.read("$.paths.['/api/v1/transfers'].post.parameters");
        assertThat(parameters).anySatisfy(parameter -> {
            assertThat(parameter.get("name")).isEqualTo("Idempotency-Key");
            assertThat(parameter.get("in")).isEqualTo("header");
            assertThat(parameter.get("required")).isEqualTo(true);
        });

        Map<?, ?> responses = (Map<?, ?>) transfer.get("responses");
        assertThat(responses.keySet().stream().map(String::valueOf).toList())
                .containsExactlyInAnyOrder("200", "201", "400", "404", "409", "422");
    }

    @Test
    @DisplayName("collection endpoints document pagination, sorting and filters")
    void collections_document_their_query_parameters() {
        List<Map<String, Object>> transactionParams =
                apiDocs.read("$.paths.['/api/v1/transactions'].get.parameters");

        List<String> names = transactionParams.stream()
                .map(parameter -> String.valueOf(parameter.get("name")))
                .toList();
        assertThat(names).contains("page", "size", "sort", "direction",
                "type", "status", "dateFrom", "dateTo", "minimumAmount", "maximumAmount");
    }

    @Test
    @DisplayName("the Scalar UI and its bundle are served locally")
    void scalar_is_served_offline() {
        RestTestClient client = RestTestClient.bindToServer()
                .baseUrl("http://localhost:" + port).build();

        client.get().uri("/docs").exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(org.springframework.http.MediaType.TEXT_HTML);

        // Served from scalar-core inside the jar, not from a CDN: the application must work offline.
        byte[] bundle = client.get().uri("/docs/scalar.js").exchange()
                .expectStatus().isOk()
                .expectBody().returnResult().getResponseBody();
        assertThat(bundle).as("the Scalar bundle must be served from the classpath").isNotEmpty();
        assertThat(bundle.length).isGreaterThan(1_000_000);
    }
}
