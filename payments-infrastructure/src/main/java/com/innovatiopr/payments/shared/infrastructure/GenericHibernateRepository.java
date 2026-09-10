package com.innovatiopr.payments.shared.infrastructure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Shared JPA plumbing for persistence-entity repositories.
 *
 * <h2>Scope: infrastructure only</h2>
 * This class exists to remove duplicated {@code EntityManager} boilerplate, and nothing more. It is
 * <em>not</em> a domain repository, and neither the domain nor the application layer may reference it —
 * an ArchUnit rule fails the build if they do.
 *
 * <p>That distinction is the whole point. A generic {@code Repository<T, ID>} exposed to the application
 * layer would force meaningless CRUD semantics onto every aggregate ({@code delete} an {@code Account}?)
 * and would leave no room for the operations that actually matter, such as
 * {@code findByIdForUpdate(AccountId)} or {@code existsByAccountNumber(AccountNumber)}. Domain repository
 * contracts stay specific and intention-revealing; this base class only removes the typing.
 *
 * <p>Note the type parameters: {@code E} is the <em>JPA entity</em>, never the aggregate. Aggregates carry
 * no persistence annotations and are mapped in and out by a dedicated mapper.
 *
 * @param <E>  the JPA entity type
 * @param <ID> the entity's primary-key type
 */
public abstract class GenericHibernateRepository<E, ID> {

    @PersistenceContext
    protected EntityManager entityManager;

    private final Class<E> entityType;

    protected GenericHibernateRepository(Class<E> entityType) {
        this.entityType = Objects.requireNonNull(entityType, "entityType");
    }

    protected Optional<E> findById(ID id) {
        return Optional.ofNullable(entityManager.find(entityType, id));
    }

    /**
     * Loads an entity and holds a row-level write lock until the transaction commits, issuing
     * {@code SELECT ... FOR UPDATE}.
     *
     * <p>Any other transaction attempting to lock the same row blocks rather than reading a stale value.
     * This is what makes concurrent transfers on the same account serialise instead of both reading the
     * same balance and both deciding there are sufficient funds.
     */
    protected Optional<E> findByIdForUpdate(ID id) {
        return Optional.ofNullable(entityManager.find(entityType, id, LockModeType.PESSIMISTIC_WRITE));
    }

    protected void persist(E entity) {
        entityManager.persist(entity);
    }

    protected E merge(E entity) {
        return entityManager.merge(entity);
    }

    protected void remove(E entity) {
        entityManager.remove(entity);
    }

    protected boolean existsById(ID id) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<E> root = query.from(entityType);
        query.select(builder.count(root)).where(builder.equal(root.get(idAttributeName()), id));
        return entityManager.createQuery(query).getSingleResult() > 0;
    }

    /** Forces pending changes to the database without committing — useful to surface constraint violations early. */
    protected void flush() {
        entityManager.flush();
    }

    /** Detaches everything so a subsequent read reloads from the database rather than the first-level cache. */
    protected void clear() {
        entityManager.clear();
    }

    protected Optional<E> findOneBy(String attribute, Object value) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<E> query = builder.createQuery(entityType);
        Root<E> root = query.from(entityType);
        query.select(root).where(builder.equal(root.get(attribute), value));
        return entityManager.createQuery(query).setMaxResults(1).getResultList().stream().findFirst();
    }

    protected boolean existsBy(String attribute, Object value) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<E> root = query.from(entityType);
        query.select(builder.count(root)).where(builder.equal(root.get(attribute), value));
        return entityManager.createQuery(query).getSingleResult() > 0;
    }

    /**
     * Offset pagination over the entity table.
     *
     * <p>Present because the brief asks the base class to offer it, but note that no read path in this
     * application uses it: every list endpoint is served by a {@code JdbcClient} read model instead.
     * Loading aggregates to render a read-only page wastes a persistence context, an N+1 risk and a
     * mapping pass on data nobody will modify.
     *
     * @param orderByAttribute must be a mapped attribute name, never client-supplied text
     */
    protected List<E> findPage(long offset, int limit, String orderByAttribute, boolean ascending) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<E> query = builder.createQuery(entityType);
        Root<E> root = query.from(entityType);
        query.select(root).orderBy(ascending
                ? builder.asc(root.get(orderByAttribute))
                : builder.desc(root.get(orderByAttribute)));
        TypedQuery<E> typed = entityManager.createQuery(query);
        typed.setFirstResult(Math.toIntExact(offset));
        typed.setMaxResults(limit);
        return typed.getResultList();
    }

    protected long count() {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        query.select(builder.count(query.from(entityType)));
        return entityManager.createQuery(query).getSingleResult();
    }

    /** Name of the identifier attribute. Overridable for entities that do not call it {@code id}. */
    protected String idAttributeName() {
        return "id";
    }

    protected Class<E> entityType() {
        return entityType;
    }
}
