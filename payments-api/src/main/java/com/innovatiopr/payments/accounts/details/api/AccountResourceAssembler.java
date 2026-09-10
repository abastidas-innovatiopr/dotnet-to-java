package com.innovatiopr.payments.accounts.details.api;

import com.innovatiopr.payments.accounts.details.application.AccountBalance;
import com.innovatiopr.payments.accounts.details.application.AccountDetails;
import com.innovatiopr.payments.shared.api.ApiPaths;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Builds account resources, with links that reflect what the account can actually do right now.
 *
 * <h2>Affordances follow domain state</h2>
 * This is the part of HATEOAS that earns its keep. A client should not have to know the rules that decide
 * whether a withdrawal is possible; it should look for a {@code withdraw} link and find one, or not.
 *
 * <ul>
 *   <li><b>ACTIVE</b> — every read link, plus {@code deposit}, {@code withdraw}, {@code transfer} and
 *       {@code freeze}. {@code close} appears only at a zero balance, because {@code Account.close()}
 *       refuses otherwise.</li>
 *   <li><b>FROZEN</b> — read links and {@code unfreeze}. No movement links at all: a frozen account can
 *       neither send nor receive, so advertising {@code deposit} would invite a request the domain is
 *       certain to reject.</li>
 *   <li><b>CLOSED</b> — read links only. Terminal state, no affordances.</li>
 * </ul>
 *
 * <p>The rules are stated once, in {@code Account}, and reflected here. The links are a projection of the
 * domain's own decisions rather than a second, drifting copy of them.
 */
@Component
public class AccountResourceAssembler {

    private static final String ACTIVE = "ACTIVE";
    private static final String FROZEN = "FROZEN";

    public AccountResource toResource(AccountDetails details, String baseUrl) {
        AccountResource resource = new AccountResource(details.id(), details.customerId(),
                details.accountNumber(), details.currency(), details.balance(), details.status(),
                details.openedAt());

        UUID id = details.id();

        // Always available, whatever the state: a closed account is still readable and auditable.
        resource.add(Link.of(ApiPaths.account(baseUrl, id), IanaLinkRelations.SELF));
        resource.add(Link.of(ApiPaths.accountBalance(baseUrl, id), "balance"));
        resource.add(Link.of(ApiPaths.accountTransactions(baseUrl, id), "transactions"));
        resource.add(Link.of(ApiPaths.accountStatement(baseUrl, id), "statement"));
        resource.add(Link.of(ApiPaths.customer(baseUrl, details.customerId()), "customer"));

        if (ACTIVE.equals(details.status())) {
            resource.add(Link.of(ApiPaths.accountDeposits(baseUrl, id), "deposit"));
            resource.add(Link.of(ApiPaths.accountWithdrawals(baseUrl, id), "withdraw"));
            resource.add(Link.of(ApiPaths.transfers(baseUrl), "transfer"));
            resource.add(Link.of(ApiPaths.accountFreeze(baseUrl, id), "freeze"));
            if (isZero(details.balance())) {
                resource.add(Link.of(ApiPaths.accountClose(baseUrl, id), "close"));
            }
        } else if (FROZEN.equals(details.status())) {
            resource.add(Link.of(ApiPaths.accountUnfreeze(baseUrl, id), "unfreeze"));
            if (isZero(details.balance())) {
                resource.add(Link.of(ApiPaths.accountClose(baseUrl, id), "close"));
            }
        }

        return resource;
    }

    public BalanceResource toBalanceResource(AccountBalance balance, String baseUrl) {
        BalanceResource resource = new BalanceResource(balance.accountId(), balance.currency(),
                balance.balance(), balance.status(), balance.asOf());
        resource.add(Link.of(ApiPaths.accountBalance(baseUrl, balance.accountId()), IanaLinkRelations.SELF));
        resource.add(Link.of(ApiPaths.account(baseUrl, balance.accountId()), "account"));
        return resource;
    }

    /** {@code compareTo}, not {@code equals}: {@code 0.00} and {@code 0} are the same balance. */
    private static boolean isZero(BigDecimal balance) {
        return balance != null && balance.compareTo(BigDecimal.ZERO) == 0;
    }
}
