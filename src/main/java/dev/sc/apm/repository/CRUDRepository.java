package dev.sc.apm.repository;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Predicate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

public abstract class CRUDRepository<E, ID extends Serializable> {
    protected final Class<E> entityType;
    protected final SessionFactory sessionFactory;

    protected CRUDRepository(Class<E> entityType, SessionFactory sessionFactory) {
        this.entityType = entityType;
        this.sessionFactory = sessionFactory;
    }

    @Transactional
    public E save(E entity) {
        Session session = sessionFactory.getCurrentSession();
        return session.merge(entity);
    }

    @Transactional
    public Optional<E> findById(ID id) {
        Session session = sessionFactory.getCurrentSession();
        E entity = session.get(entityType, id);
        return entity == null ? Optional.empty() : Optional.of(entity);
    }

    @Transactional
    public List<E> findAll() {
        Session session = sessionFactory.getCurrentSession();
        CriteriaBuilder builder = session.getCriteriaBuilder();

        CriteriaQuery<E> query = builder.createQuery(entityType);
        Root<E> root = query.from(entityType);
        query.select(root);

        return session.createQuery(query).getResultList();
    }

    @Transactional
    public Page<E> findAll(Pageable pageable) {
        return findAllBy(pageable, (builder, root) -> new Predicate[0]);
    }

    @Transactional
    public List<E> findAllBy(BiFunction<CriteriaBuilder, Root<E>, Predicate[]> predicateBuilder) {
        return getQueryFindAllBy(predicateBuilder).getResultList();
    }

    private Query<E> getQueryFindAllBy(BiFunction<CriteriaBuilder, Root<E>, Predicate[]> predicateBuilder) {
        Session session = sessionFactory.getCurrentSession();
        CriteriaBuilder builder = session.getCriteriaBuilder();

        CriteriaQuery<E> query = builder.createQuery(entityType);
        Root<E> root = query.from(entityType);

        Predicate[] predicates = predicateBuilder.apply(builder, root);

        query.select(root).where(predicates);

        return session.createQuery(query);
    }

    @Transactional
    public Page<E> findAllBy(Pageable pageable, BiFunction<CriteriaBuilder, Root<E>, Predicate[]> predicateBuilder) {

        long total = countBy(predicateBuilder);

        // Calc max page
        int pageSize = pageable.size();
        int maxPage = (int) Math.ceil((double) total / pageSize);
        int currentPage = Math.min(pageable.page(), Math.max(maxPage, 1));

        int offset = (currentPage - 1) * pageSize;

        List<E> content = getQueryFindAllBy(predicateBuilder)
                .setFirstResult(offset)
                .setMaxResults(pageSize)
                .getResultList();

        return new Page<>(
                currentPage,
                content.size(),
                total,
                content
        );
    }

    @Transactional
    public long countBy(BiFunction<CriteriaBuilder, Root<E>, Predicate[]> predicateBuilder) {
        Session session = sessionFactory.getCurrentSession();
        CriteriaBuilder builder = session.getCriteriaBuilder();

        CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
        Root<E> root = countQuery.from(entityType);

        Predicate[] predicates = predicateBuilder.apply(builder, root);

        countQuery.select(builder.count(root)).where(predicates);

        return session.createQuery(countQuery).getSingleResult();
    }

    @Transactional
    public long count() {
        return countBy((builder, root) -> new Predicate[0]);
    }

    @Transactional
    public void deleteById(ID id) {
        sessionFactory.getCurrentSession()
                .remove(findById(id));
    }

    @Transactional
    protected void clearAll() {
        sessionFactory.getCurrentSession()
                .createMutationQuery(
                        "DELETE FROM " + entityType.getSimpleName()
                )
                .executeUpdate();
    }
}
