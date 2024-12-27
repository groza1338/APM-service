package dev.sc.apm.repository;

import dev.sc.apm.entity.Client;
import jakarta.persistence.criteria.Predicate;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;


@Repository
public class ClientRepository extends CRUDRepository<Client, Long> implements ClearableRepository {
    public ClientRepository(SessionFactory sessionFactory) {
        super(Client.class, sessionFactory);
    }

    @Transactional
    public Optional<Client> findByPassport(String passport) {
        return findAllBy((builder, root) ->
                new Predicate[]{builder.equal(root.get("passport"), passport)}
        ).stream().findFirst();
    }

    @Override
    @Transactional
    public void clearAll() {
        super.clearAll();
    }
}
