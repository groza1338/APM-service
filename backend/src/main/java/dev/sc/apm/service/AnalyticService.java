package dev.sc.apm.service;

import dev.sc.apm.entity.CreditAgreement;
import dev.sc.apm.entity.CreditApplication;
import dev.sc.apm.entity.CreditApplicationStatus;
import dev.sc.apm.entity.SigningStatus;
import dev.sc.apm.repository.CreditAgreementRepository;
import dev.sc.apm.repository.CreditApplicationRepository;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AnalyticService {

    private final CreditApplicationRepository creditApplicationRepository;
    private final CreditAgreementRepository creditAgreementRepository;
    private final Random random = new Random(); // united for test

    @Transactional
    @Retryable(
            retryFor = {OptimisticLockException.class}
    )
    public void evaluateCreditApplication(@NotNull Long creditApplicationId) {

        CreditApplication application = creditApplicationRepository.findById(creditApplicationId).orElseThrow(
                () -> new NoSuchElementException("Credit application with id " + creditApplicationId + " not found")
        );

        if (!isAnalyticsRequired(application)) {
            return;
        }

        // some logic
        CreditApplicationStatus status = approveCredit() ?
                CreditApplicationStatus.APPROVED : CreditApplicationStatus.REJECTED;

        application.setStatus(status);

        if (status == CreditApplicationStatus.REJECTED) {
            return;
        }

        BigDecimal approvedAmount = BigDecimal.valueOf(
                application.getRequestedAmount().multiply(BigDecimal.valueOf(0.9)).doubleValue()
        );
        Integer approvedTerm = 30 + random.nextInt(336); // 336 = 365 - 30 + 1

        application.setApprovedTerm(approvedTerm);
        application.setApprovedAmount(approvedAmount);

        CreditAgreement agreement = CreditAgreement.builder()
                .application(application)
                .signingStatus(SigningStatus.NOT_SIGNED)
                .build();

        CreditAgreement saved;
        try {
            saved = creditAgreementRepository.save(agreement);
        } catch (DataIntegrityViolationException e) {
            if (
                    e.getCause() != null &&
                            e.getCause() instanceof ConstraintViolationException cause &&
                            cause.getConstraintName().equals("credit_agreement_credit_application_id_key")
            ) {
                throw new OptimisticLockException("Agreement for application " + application.getId() + " already exists");
            }
            throw e;
        }

        application.setCreditAgreement(saved);

        // mb send event in MQ
    }

    private boolean isAnalyticsRequired(CreditApplication application) {
        return application.getStatus() == CreditApplicationStatus.PENDING && application.getCreditAgreement() == null;
    }

    // exists only for testability
    public boolean approveCredit() {
        return random.nextBoolean();
    }
}
