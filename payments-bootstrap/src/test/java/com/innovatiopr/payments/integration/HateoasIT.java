package com.innovatiopr.payments.integration;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.accounts.AccountsApi;
import com.innovatiopr.payments.customers.CustomerId;
import com.innovatiopr.payments.customers.CustomersApi;
import com.innovatiopr.payments.payments.PaymentsApi;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hypermedia behaviour: link structure, and the rule that advertised actions must match domain state.
 *
 * <p>The point of these assertions is that a client should never have to encode the bank's rules. If a
 * withdrawal is impossible, the link is absent — and the response says so before the client tries.
 */
class HateoasIT extends AbstractIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    CustomersApi customers;

    @Autowired
    AccountsApi accounts;

    @Autowired
    PaymentsApi payments;

    private RestTestClient client;
    private AccountId account;
    private CustomerId customer;

    @BeforeEach
    void openAnAccount() {
        client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
        customer = customers.register("Edsger", "Dijkstra", "edsger@example.com");
        account = accounts.open(customer, "USD");
        payments.deposit(account, new BigDecimal("500.00"), "USD", "Opening deposit");
    }

    private DocumentContext get(String uri, Object... args) {
        byte[] body = client.get().uri(uri, args).exchange()
                .expectStatus().isOk()
                .expectBody().returnResult().getResponseBody();
        return JsonPath.using(Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS))
                .parse(new String(body, StandardCharsets.UTF_8));
    }

    private DocumentContext post(String uri, Object... args) {
        byte[] body = client.post().uri(uri, args).exchange()
                .expectStatus().isOk()
                .expectBody().returnResult().getResponseBody();
        return JsonPath.using(Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS))
                .parse(new String(body, StandardCharsets.UTF_8));
    }

    private static java.util.Set<String> relations(DocumentContext json) {
        Map<String, Object> links = json.read("$._links");
        return links.keySet();
    }

    @Test
    @DisplayName("responses are HAL and carry a self link")
    void serves_hal_json() {
        client.get().uri("/api/v1/accounts/{id}", account.value())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.valueOf("application/hal+json"));

        assertThat((String) get("/api/v1/accounts/{id}", account.value()).read("$._links.self.href"))
                .endsWith("/api/v1/accounts/" + account.value());
    }

    @Test
    @DisplayName("an ACTIVE account advertises every action it can perform")
    void active_account_links() {
        var json = get("/api/v1/accounts/{id}", account.value());

        assertThat(relations(json))
                .contains("self", "balance", "transactions", "statement", "customer")
                .contains("deposit", "withdraw", "transfer", "freeze");
    }

    @Test
    @DisplayName("an ACTIVE account with a balance does not advertise close")
    void close_appears_only_at_a_zero_balance() {
        assertThat(relations(get("/api/v1/accounts/{id}", account.value())))
                .as("Account.close() refuses a non-zero balance, so the link must not be offered")
                .doesNotContain("close");

        payments.withdraw(account, new BigDecimal("500.00"), "USD", "Empty it");

        assertThat(relations(get("/api/v1/accounts/{id}", account.value()))).contains("close");
    }

    @Test
    @DisplayName("a FROZEN account loses every movement link and gains unfreeze")
    void frozen_account_links() {
        var json = post("/api/v1/accounts/{id}/freeze", account.value());

        assertThat((String) json.read("$.status")).isEqualTo("FROZEN");
        assertThat(relations(json))
                .as("a frozen account can neither send nor receive")
                .doesNotContain("deposit", "withdraw", "transfer", "freeze");
        assertThat(relations(json))
                .as("read links stay available and the reversing action is offered")
                .contains("self", "balance", "transactions", "statement", "unfreeze");
    }

    @Test
    @DisplayName("unfreezing restores the action links")
    void unfreeze_restores_links() {
        post("/api/v1/accounts/{id}/freeze", account.value());
        var json = post("/api/v1/accounts/{id}/unfreeze", account.value());

        assertThat((String) json.read("$.status")).isEqualTo("ACTIVE");
        assertThat(relations(json)).contains("deposit", "withdraw", "transfer", "freeze");
        assertThat(relations(json)).doesNotContain("unfreeze");
    }

    @Test
    @DisplayName("a CLOSED account advertises no actions at all")
    void closed_account_links() {
        payments.withdraw(account, new BigDecimal("500.00"), "USD", "Empty it");
        var json = post("/api/v1/accounts/{id}/close", account.value());

        assertThat((String) json.read("$.status")).isEqualTo("CLOSED");
        assertThat(relations(json))
                .doesNotContain("deposit", "withdraw", "transfer", "freeze", "unfreeze", "close");
        assertThat(relations(json))
                .as("a closed account remains readable and auditable")
                .contains("self", "balance", "transactions", "statement");
    }

    @Test
    @DisplayName("the links an account advertises match what the API will actually accept")
    void advertised_links_are_honest() {
        // Freeze, then follow the (absent) withdraw affordance anyway: the API must refuse, which is
        // what makes the link's absence meaningful rather than decorative.
        post("/api/v1/accounts/{id}/freeze", account.value());

        client.post().uri("/api/v1/accounts/{id}/withdrawals", account.value())
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"amount\":1.00,\"currency\":\"USD\"}")
                .exchange()
                .expectStatus().isEqualTo(422);
    }

    @Test
    @DisplayName("a transfer response links to the transaction and both accounts")
    void transfer_resource_links() {
        AccountId destination = accounts.open(customer, "USD");

        byte[] body = client.post().uri("/api/v1/transfers")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"sourceAccountId":"%s","destinationAccountId":"%s",
                         "amount":25.00,"currency":"USD","reference":"Rent"}
                        """.formatted(account.value(), destination.value()))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("Location")
                .expectBody().returnResult().getResponseBody();

        DocumentContext json = JsonPath.parse(new String(body, StandardCharsets.UTF_8));
        assertThat(relations(json)).contains("self", "transaction", "sourceAccount", "destinationAccount");
    }

    @Test
    @DisplayName("a statement line links back to the transaction that produced it")
    void statement_lines_link_to_their_transaction() {
        var json = get("/api/v1/accounts/{id}/statement?size=5", account.value());

        assertThat((String) json.read("$._embedded.statementLineResourceList[0]._links.transaction.href"))
                .contains("/api/v1/transactions/");
    }

    @Test
    @DisplayName("a customer resource links to itself and to its accounts")
    void customer_resource_links() {
        assertThat(relations(get("/api/v1/customers/{id}", customer.value())))
                .contains("self", "accounts");
    }

    @Test
    @DisplayName("errors are RFC 9457 Problem Details with a stable code")
    void problem_details_shape() {
        byte[] body = client.get().uri("/api/v1/accounts/{id}", UUID.randomUUID())
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody().returnResult().getResponseBody();

        DocumentContext json = JsonPath.parse(new String(body, StandardCharsets.UTF_8));
        assertThat((String) json.read("$.code")).isEqualTo("ACCOUNT_NOT_FOUND");
        assertThat((int) json.read("$.status")).isEqualTo(404);
        assertThat(json.<Object>read("$.title")).isNotNull();
        assertThat(json.<Object>read("$.detail")).isNotNull();
        assertThat(json.<Object>read("$.correlationId")).as("operators need to find this request in the logs")
                .isNotNull();
    }

    @Test
    @DisplayName("a correlation id supplied by the client is echoed back")
    void echoes_the_correlation_id() {
        client.get().uri("/api/v1/accounts/{id}", account.value())
                .header("X-Correlation-Id", "client-supplied-123")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Correlation-Id", "client-supplied-123");
    }
}
