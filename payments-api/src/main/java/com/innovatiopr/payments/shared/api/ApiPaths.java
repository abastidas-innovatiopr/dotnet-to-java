package com.innovatiopr.payments.shared.api;

import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

/**
 * Every URI this API exposes, in one place.
 *
 * <h2>Why not {@code linkTo(methodOn(...))}</h2>
 * Spring HATEOAS's most familiar link builder reflects over an annotated {@code @RestController} method to
 * discover its mapping. With functional routing there is no annotated method to point at, so that
 * technique is simply unavailable — a real and under-documented trade-off of choosing {@code RouterFunction}
 * over {@code @RestController}.
 *
 * <p>The replacement is this class: path templates declared once and used by both the routes and the
 * assemblers. It is arguably safer than the reflective builder, since a renamed path breaks compilation
 * in one place rather than silently producing a wrong link at runtime — but it is a deliberate substitute,
 * not an accident.
 */
public final class ApiPaths {

    public static final String API_V1 = "/api/v1";

    public static final String CUSTOMERS = API_V1 + "/customers";
    public static final String CUSTOMER = CUSTOMERS + "/{customerId}";

    public static final String ACCOUNTS = API_V1 + "/accounts";
    public static final String ACCOUNT = ACCOUNTS + "/{accountId}";
    public static final String ACCOUNT_BALANCE = ACCOUNT + "/balance";
    public static final String ACCOUNT_DEPOSITS = ACCOUNT + "/deposits";
    public static final String ACCOUNT_WITHDRAWALS = ACCOUNT + "/withdrawals";
    public static final String ACCOUNT_TRANSACTIONS = ACCOUNT + "/transactions";
    public static final String ACCOUNT_STATEMENT = ACCOUNT + "/statement";
    public static final String ACCOUNT_FREEZE = ACCOUNT + "/freeze";
    public static final String ACCOUNT_UNFREEZE = ACCOUNT + "/unfreeze";
    public static final String ACCOUNT_CLOSE = ACCOUNT + "/close";

    public static final String TRANSFERS = API_V1 + "/transfers";
    public static final String TRANSACTIONS = API_V1 + "/transactions";
    public static final String TRANSACTION = TRANSACTIONS + "/{transactionId}";

    private ApiPaths() {
    }

    /** Scheme, host and port of the current request — links are absolute so clients can follow them blindly. */
    public static String baseUrl(ServerRequest request) {
        return UriComponentsBuilder.fromUri(request.uri())
                .replacePath(null)
                .replaceQuery(null)
                .fragment(null)
                .build()
                .toUriString();
    }

    public static String customer(String baseUrl, UUID customerId) {
        return baseUrl + CUSTOMERS + "/" + customerId;
    }

    public static String customers(String baseUrl) {
        return baseUrl + CUSTOMERS;
    }

    public static String account(String baseUrl, UUID accountId) {
        return baseUrl + ACCOUNTS + "/" + accountId;
    }

    public static String accounts(String baseUrl) {
        return baseUrl + ACCOUNTS;
    }

    public static String accountBalance(String baseUrl, UUID accountId) {
        return account(baseUrl, accountId) + "/balance";
    }

    public static String accountDeposits(String baseUrl, UUID accountId) {
        return account(baseUrl, accountId) + "/deposits";
    }

    public static String accountWithdrawals(String baseUrl, UUID accountId) {
        return account(baseUrl, accountId) + "/withdrawals";
    }

    public static String accountTransactions(String baseUrl, UUID accountId) {
        return account(baseUrl, accountId) + "/transactions";
    }

    public static String accountStatement(String baseUrl, UUID accountId) {
        return account(baseUrl, accountId) + "/statement";
    }

    public static String accountFreeze(String baseUrl, UUID accountId) {
        return account(baseUrl, accountId) + "/freeze";
    }

    public static String accountUnfreeze(String baseUrl, UUID accountId) {
        return account(baseUrl, accountId) + "/unfreeze";
    }

    public static String accountClose(String baseUrl, UUID accountId) {
        return account(baseUrl, accountId) + "/close";
    }

    public static String transfers(String baseUrl) {
        return baseUrl + TRANSFERS;
    }

    public static String transaction(String baseUrl, UUID transactionId) {
        return baseUrl + TRANSACTIONS + "/" + transactionId;
    }

    public static String transactions(String baseUrl) {
        return baseUrl + TRANSACTIONS;
    }
}
