package dev.sc.apm.repository;

import dev.sc.apm.entity.CreditApplication;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class CreditApplicationRepository extends CRUDRepository<CreditApplication, Long> implements ClearableRepository {
    public CreditApplicationRepository(SessionFactory sessionFactory) {
        super(CreditApplication.class, sessionFactory);
    }

    @Override
    @Transactional
    public void clearAll() {
        super.clearAll();
    }
}
