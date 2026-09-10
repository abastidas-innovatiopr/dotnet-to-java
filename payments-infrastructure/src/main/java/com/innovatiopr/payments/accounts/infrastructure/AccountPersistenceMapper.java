package com.innovatiopr.payments.accounts.infrastructure;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.accounts.domain.Account;
import com.innovatiopr.payments.accounts.domain.AccountNumber;
import com.innovatiopr.payments.accounts.domain.AccountStatus;
import com.innovatiopr.payments.customers.CustomerId;
import com.innovatiopr.payments.shared.domain.Money;

import java.util.Currency;

/** Translates between the {@code Account} aggregate and its JPA representation. */
final class AccountPersistenceMapper {

    private AccountPersistenceMapper() {
    }

    static Account toDomain(AccountJpaEntity entity) {
        Currency currency = Currency.getInstance(entity.getCurrency());
        return Account.reconstitute(
                AccountId.of(entity.getId()),
                CustomerId.of(entity.getCustomerId()),
                AccountNumber.fromStorage(entity.getAccountNumber()),
                currency,
                Money.of(entity.getBalance(), currency),
                AccountStatus.valueOf(entity.getStatus()),
                entity.getOpenedAt(),
                entity.getVersion());
    }

    static AccountJpaEntity toNewEntity(Account account) {
        return new AccountJpaEntity(
                account.id().value(),
                account.customerId().value(),
                account.accountNumber().value(),
                account.currency().getCurrencyCode(),
                account.balance().amount(),
                account.status().name(),
                account.openedAt(),
                account.version());
    }

    /**
     * Copies mutable state onto a managed entity.
     *
     * <p>The entity is already in the persistence context, so Hibernate's dirty checking notices the
     * change and emits the {@code UPDATE} at flush time. There is no {@code merge} and no explicit
     * {@code save} call — this is one of the larger differences from EF Core, where a detached graph
     * usually has to be reattached explicitly.
     */
    static void applyTo(AccountJpaEntity entity, Account account) {
        entity.setBalance(account.balance().amount());
        entity.setStatus(account.status().name());
    }
}
