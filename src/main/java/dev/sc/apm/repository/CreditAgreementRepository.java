package dev.sc.apm.repository;

import dev.sc.apm.entity.CreditAgreement;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class CreditAgreementRepository extends CRUDRepository<CreditAgreement, Long> implements ClearableRepository {
    public CreditAgreementRepository(SessionFactory sessionFactory) {
        super(CreditAgreement.class, sessionFactory);
    }

    @Override
    @Transactional
    public void clearAll() {
        super.clearAll();
    }
}
