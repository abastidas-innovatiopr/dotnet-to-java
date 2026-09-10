package com.innovatiopr.payments.accounts.infrastructure;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.accounts.application.AccountRepository;
import com.innovatiopr.payments.accounts.domain.Account;
import com.innovatiopr.payments.accounts.domain.AccountNumber;
import com.innovatiopr.payments.shared.infrastructure.GenericHibernateRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA-backed implementation of the Accounts persistence port.
 *
 * <p>Shows why a generic base class must not become the whole contract: {@link #findByIdForUpdate} is the
 * operation the transfer use case depends on, and no generic CRUD interface would have thought to offer it.
 */
@Repository
class HibernateAccountRepository extends GenericHibernateRepository<AccountJpaEntity, UUID>
        implements AccountRepository {

    HibernateAccountRepository() {
        super(AccountJpaEntity.class);
    }

    @Override
    public Optional<Account> findById(AccountId id) {
        return findById(id.value()).map(AccountPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Account> findByIdForUpdate(AccountId id) {
        return findByIdForUpdate(id.value()).map(AccountPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Account> findByAccountNumber(AccountNumber accountNumber) {
        return findOneBy("accountNumber", accountNumber.value()).map(AccountPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsById(AccountId id) {
        return existsById(id.value());
    }

    @Override
    public boolean existsByAccountNumber(AccountNumber accountNumber) {
        return existsBy("accountNumber", accountNumber.value());
    }

    @Override
    public void save(Account account) {
        Optional<AccountJpaEntity> existing = findById(account.id().value());
        if (existing.isPresent()) {
            AccountPersistenceMapper.applyTo(existing.get(), account);
        } else {
            persist(AccountPersistenceMapper.toNewEntity(account));
        }
    }
}
