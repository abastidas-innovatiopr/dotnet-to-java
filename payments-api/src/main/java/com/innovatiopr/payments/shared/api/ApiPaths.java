package com.innovatiopr.payments.shared.api;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

/**
 * Every URI this API exposes, in one place. The constants are the {@code @RequestMapping} values on the
 * controllers and the source of every hypermedia link, so a path exists exactly once in the codebase.
 *
 * <h2>Why not {@code linkTo(methodOn(...))}</h2>
 * Spring HATEOAS's familiar reflective builder <em>is</em> available now that the endpoints are annotated
 * controllers — but only for links that stay inside one module. {@code AccountResourceAssembler} emits
 * links into {@code customers}, {@code payments} and {@code ledger}, and naming those controllers would
 * import another module's internal package: legal Java that {@code ModularityTest} fails the build on,
 * and rightly, since it is exactly the coupling the module boundaries exist to prevent.
 *
 * <p>Since some links cannot use the reflective builder, all of them use this class instead. One
 * mechanism that always works beats two that each work half the time and leave the reader deciding which
 * applies. It also fails earlier: a renamed path breaks compilation here rather than silently producing a
 * wrong link at runtime.
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

    /**
     * Scheme, host and port of the current request — links are absolute so clients can follow them blindly.
     *
     * <p>Reads the request from {@code RequestContextHolder} rather than taking it as a parameter, which
     * keeps the assemblers free of transport types. Only callable on a request-handling thread.
     */
    public static String baseUrl() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
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
