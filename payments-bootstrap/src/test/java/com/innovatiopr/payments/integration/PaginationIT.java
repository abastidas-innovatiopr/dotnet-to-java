package com.innovatiopr.payments.integration;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.accounts.AccountsApi;
import com.innovatiopr.payments.customers.CustomerId;
import com.innovatiopr.payments.customers.CustomersApi;
import com.innovatiopr.payments.payments.PaymentsApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pagination over HTTP: metadata, navigation links, ordering and filter preservation.
 *
 * <p>Uses {@code RestTestClient}, which replaces {@code TestRestTemplate} in Spring Boot 4.
 */
class PaginationIT extends AbstractIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    CustomersApi customers;

    @Autowired
    AccountsApi accounts;

    @Autowired
    PaymentsApi payments;

    private RestTestClient client;
    private AccountId source;

    @BeforeEach
    void seedTransactions() {
        client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();

        CustomerId customer = customers.register("Alan", "Turing", "alan@example.com");
        source = accounts.open(customer, "USD");
        AccountId destination = accounts.open(customer, "USD");
        payments.deposit(source, new BigDecimal("10000.00"), "USD", "Opening deposit");

        // 25 transfers + 1 deposit = 26 transactions, so the default page size of 20 yields two pages.
        for (int i = 1; i <= 25; i++) {
            payments.transfer(UUID.randomUUID().toString(), source, destination,
                    new BigDecimal(i + ".00"), "USD", "Transfer " + i);
        }
    }

    /**
     * Reads a response as JSON.
     *
     * <p>Configured with {@code SUPPRESS_EXCEPTIONS} so that a missing path yields {@code null} rather
     * than throwing. Several assertions here are about a link being <em>absent</em> — no {@code next} on
     * the last page — and absence is the thing under test, not an error.
     */
    private com.jayway.jsonpath.DocumentContext get(String uri) {
        byte[] body = client.get().uri(uri).exchange()
                .expectStatus().isOk()
                .expectBody().returnResult().getResponseBody();

        com.jayway.jsonpath.Configuration configuration = com.jayway.jsonpath.Configuration.defaultConfiguration()
                .addOptions(com.jayway.jsonpath.Option.SUPPRESS_EXCEPTIONS);
        return com.jayway.jsonpath.JsonPath.using(configuration)
                .parse(new String(body, java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("the first page reports totals and offers next but not previous")
    void first_page() {
        var json = get("/api/v1/transactions?page=0&size=10");

        assertThat((int) json.read("$.page.number")).isZero();
        assertThat((int) json.read("$.page.size")).isEqualTo(10);
        assertThat(((Number) json.read("$.page.totalElements")).longValue()).isEqualTo(26);
        assertThat((int) json.read("$.page.totalPages")).isEqualTo(3);
        assertThat((List<?>) json.read("$._embedded.transactionResourceList")).hasSize(10);

        assertThat(json.<Object>read("$._links.self.href")).isNotNull();
        assertThat(json.<Object>read("$._links.first.href")).isNotNull();
        assertThat(json.<Object>read("$._links.last.href")).isNotNull();
        assertThat(json.<Object>read("$._links.next.href")).isNotNull();
        assertThat(json.<Object>read("$._links.prev")).as("no previous page exists").isNull();
    }

    @Test
    @DisplayName("a middle page offers both directions")
    void middle_page() {
        var json = get("/api/v1/transactions?page=1&size=10");

        assertThat((int) json.read("$.page.number")).isEqualTo(1);
        assertThat(json.<Object>read("$._links.prev.href")).isNotNull();
        assertThat(json.<Object>read("$._links.next.href")).isNotNull();
    }

    @Test
    @DisplayName("the last page offers previous but not next, and may be partial")
    void last_page() {
        var json = get("/api/v1/transactions?page=2&size=10");

        assertThat((List<?>) json.read("$._embedded.transactionResourceList")).hasSize(6);
        assertThat(json.<Object>read("$._links.prev.href")).isNotNull();
        assertThat(json.<Object>read("$._links.next")).as("no next page exists").isNull();
    }

    @Test
    @DisplayName("a page beyond the end is empty and advertises no next")
    void page_past_the_end() {
        var json = get("/api/v1/transactions?page=99&size=10");

        assertThat(json.<Object>read("$._embedded")).isNull();
        assertThat(json.<Object>read("$._links.next")).isNull();
        assertThat(((Number) json.read("$.page.totalElements")).longValue()).isEqualTo(26);
    }

    @Test
    @DisplayName("an oversized page request is clamped to the maximum")
    void clamps_the_page_size() {
        var json = get("/api/v1/transactions?page=0&size=1000");

        assertThat((int) json.read("$.page.size"))
                .as("no client may request an unbounded result set")
                .isEqualTo(100);
        assertThat((String) json.read("$._links.self.href")).contains("size=100");
    }

    @Test
    @DisplayName("a nonsensical page size falls back to the default")
    void normalises_an_invalid_page_size() {
        assertThat((int) get("/api/v1/transactions?size=0").read("$.page.size")).isEqualTo(20);
        assertThat((int) get("/api/v1/transactions?size=-7").read("$.page.size")).isEqualTo(20);
        assertThat((int) get("/api/v1/transactions?page=-3").read("$.page.number")).isZero();
        assertThat((int) get("/api/v1/transactions?size=banana").read("$.page.size")).isEqualTo(20);
    }

    @Test
    @DisplayName("ordering is deterministic, so paging enumerates every row exactly once")
    void paging_enumerates_without_gaps_or_duplicates() {
        // The seeded transfers share a timestamp to the millisecond, so without the id tiebreaker in
        // ORDER BY, LIMIT/OFFSET could return the same row on two pages and skip another entirely.
        List<String> collected = new java.util.ArrayList<>();
        for (int page = 0; page < 3; page++) {
            List<String> ids = get("/api/v1/transactions?page=" + page + "&size=10")
                    .read("$._embedded.transactionResourceList[*].id");
            collected.addAll(ids);
        }

        assertThat(collected).hasSize(26);
        assertThat(collected).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("filters apply to the page and to its total, and survive into the links")
    void filters_are_preserved_across_pages() {
        var json = get("/api/v1/transactions?type=TRANSFER&page=0&size=10");

        // 25 transfers, so the deposit must be excluded from the count as well as from the rows.
        assertThat(((Number) json.read("$.page.totalElements")).longValue()).isEqualTo(25);
        assertThat((int) json.read("$.page.totalPages")).isEqualTo(3);

        String next = json.read("$._links.next.href");
        assertThat(next).contains("type=TRANSFER").contains("page=1").contains("size=10");

        List<String> types = json.read("$._embedded.transactionResourceList[*].type");
        assertThat(types).containsOnly("TRANSFER");
    }

    @Test
    @DisplayName("amount filters narrow the result set correctly")
    void filters_by_amount_range() {
        var json = get("/api/v1/transactions?type=TRANSFER&minimumAmount=20&maximumAmount=25&size=100");

        List<Number> amounts = json.read("$._embedded.transactionResourceList[*].amount");
        assertThat(amounts).isNotEmpty();
        assertThat(amounts).allSatisfy(amount ->
                assertThat(new BigDecimal(amount.toString()))
                        .isBetween(new BigDecimal("20"), new BigDecimal("25")));
    }

    @Test
    @DisplayName("sorting uses the allow-list and ignores unknown fields")
    void sorting_is_whitelisted() {
        List<Number> ascending = get("/api/v1/transactions?type=TRANSFER&sort=amount&direction=asc&size=5")
                .read("$._embedded.transactionResourceList[*].amount");
        assertThat(ascending).isSortedAccordingTo(
                java.util.Comparator.comparing(n -> new BigDecimal(n.toString())));

        List<Number> descending = get("/api/v1/transactions?type=TRANSFER&sort=amount&direction=desc&size=5")
                .read("$._embedded.transactionResourceList[*].amount");
        assertThat(descending).isSortedAccordingTo(
                java.util.Comparator.<Number, BigDecimal>comparing(n -> new BigDecimal(n.toString())).reversed());

        // An unknown or hostile sort field falls back to the default rather than reaching the SQL.
        var injected = get("/api/v1/transactions?sort=amount;DROP%20TABLE%20accounts--&size=5");
        assertThat((List<?>) injected.read("$._embedded.transactionResourceList")).hasSize(5);
        assertThat(count("accounts")).isEqualTo(2);
    }

    @Test
    @DisplayName("account transaction history is paginated under the account resource")
    void account_scoped_history_is_paginated() {
        var json = get("/api/v1/accounts/" + source.value() + "/transactions?page=0&size=5");

        assertThat((int) json.read("$.page.size")).isEqualTo(5);
        assertThat(((Number) json.read("$.page.totalElements")).longValue()).isEqualTo(26);
        assertThat((String) json.read("$._links.next.href")).contains("/accounts/" + source.value());
    }

    @Test
    @DisplayName("history for an unknown account is 404, not an empty page")
    void unknown_account_history_is_not_found() {
        client.get().uri("/api/v1/accounts/{id}/transactions", UUID.randomUUID())
                .exchange()
                .expectStatus().isNotFound();
    }
}
