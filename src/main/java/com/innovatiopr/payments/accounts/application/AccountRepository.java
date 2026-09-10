package com.innovatiopr.payments.accounts.application;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.accounts.domain.Account;
import com.innovatiopr.payments.accounts.domain.AccountNumber;

import java.util.Optional;

/**
 * The Accounts module's persistence port.
 *
 * <p>Domain-oriented, not generic. There is no {@code Repository<T, ID>} that every aggregate must
 * implement: a generic CRUD contract forces meaningless operations onto aggregates that should not have
 * them (nothing should {@code delete} an {@code Account}) and gives no place to express the operations
 * that actually matter, such as {@link #findByIdForUpdate}.
 *
 * <p>Nothing here mentions {@code JpaRepository}, {@code EntityManager}, {@code Page} or {@code Pageable}.
 * The application layer must be able to compile without Hibernate on the classpath, and an ArchUnit rule
 * enforces it.
 */
public interface AccountRepository {

    Optional<Account> findById(AccountId id);

    /**
     * Loads an account and holds a row-level write lock until the surrounding transaction commits
     * ({@code SELECT ... FOR UPDATE}). Used by every money movement — see the concurrency notes in
     * {@code README.md}.
     */
    Optional<Account> findByIdForUpdate(AccountId id);

    Optional<Account> findByAccountNumber(AccountNumber accountNumber);

    boolean existsById(AccountId id);

    boolean existsByAccountNumber(AccountNumber accountNumber);

    void save(Account account);
}
