package dev.sc.apm.service;


import dev.sc.apm.dto.*;
import dev.sc.apm.entity.*;
import dev.sc.apm.exception.ClientInfoMismatchException;
import dev.sc.apm.exception.CreditApplicationNotFound;
import dev.sc.apm.exception.PendingStatusDuringSigningException;
import dev.sc.apm.exception.RejectedStatusDuringSigningException;
import dev.sc.apm.mapper.ClientMapper;
import dev.sc.apm.mapper.CreditApplicationMapper;
import dev.sc.apm.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.Optional;

import static dev.sc.apm.service.ServiceUtil.getPageResponse;

@Service
@Validated
public class APMService {

    private final AnalyticService analyticService;

    private final ClientRepository clientRepository;
    private final CreditApplicationRepository creditApplicationRepository;
    private final CreditAgreementRepository creditAgreementRepository;

    private final ClientMapper clientMapper;
    private final CreditApplicationMapper creditApplicationMapper;

    private final int APPLICATION_PAGE_SIZE;

    public APMService(
            AnalyticService analyticService,
            ClientRepository clientRepository,
            CreditApplicationRepository creditApplicationRepository,
            CreditAgreementRepository creditAgreementRepository,
            ClientMapper clientMapper,
            CreditApplicationMapper creditApplicationMapper,
            @Qualifier("defaultPageSize") int applicationPageSize
    ) {
        this.analyticService = analyticService;
        this.clientRepository = clientRepository;
        this.creditApplicationRepository = creditApplicationRepository;
        this.creditAgreementRepository = creditAgreementRepository;
        this.clientMapper = clientMapper;
        this.creditApplicationMapper = creditApplicationMapper;
        APPLICATION_PAGE_SIZE = applicationPageSize;
    }

    @Transactional
    public CreditApplicationDto createCreditApplication(@Valid CreditApplicationRequestDto creditRequestDto) {

        Optional<Client> clientOptional = clientRepository.findByPassport(
                creditRequestDto.getApplicant().getPassport()
        );
        /*
         * The logic of creating a client can also be moved to the ClientService.Then get the jpa entity from the L1 cache
         * */
        Client client;
        if (clientOptional.isEmpty()) {
            Client creatable = clientMapper.fromCreditApplicantDto(creditRequestDto.getApplicant());
            client = trySaveClient(creatable);
        } else {
            client = clientOptional.get();
        }

        CreditApplicantDto storedClient = clientMapper.toCreditApplicantDto(client);

        if (!storedClient.equals(creditRequestDto.getApplicant())) {
            throw new ClientInfoMismatchException();
        }

        CreditApplication creditApplication = CreditApplication.builder()
                .client(client)
                .status(CreditApplicationStatus.PENDING)
                .requestedAmount(creditRequestDto.getAmount())
                .build();

        CreditApplication savedCreditApplication = creditApplicationRepository.save(creditApplication);

        // would be possible to send an async event to MQ instead it
        analyticService.evaluateCreditApplication(savedCreditApplication.getId());

        CreditApplication processedApplication = creditApplicationRepository.findById(savedCreditApplication.getId())
                .orElseThrow(() -> new IllegalStateException("Credit application not found"));

        CreditApplicationDto applicationDto = creditApplicationMapper.fromCreditApplication(processedApplication);

        return applicationDto;
    }

    private Client trySaveClient(Client client) {
        /*
         * in case of creating clients from several simultaneous threads/requests
         * */
        try {
            return clientRepository.save(client);
        } catch (DataIntegrityViolationException e) {
            if (
                    e.getCause() != null &&
                            e.getCause() instanceof ConstraintViolationException cause &&
                            cause.getConstraintName().equals("client_passport_key")
            ) {
                return clientRepository.findByPassport(client.getPassport()).get();
            }
            throw e;
        }
    }

    @Transactional
    public CreditAgreementDto signCreditAgreement(@Valid @Positive long applicationId) {
        CreditApplication creditApplication = creditApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new CreditApplicationNotFound(applicationId));

        switch (creditApplication.getStatus()) {
            case PENDING -> throw new PendingStatusDuringSigningException(applicationId);
            case REJECTED -> throw new RejectedStatusDuringSigningException(applicationId);
        }

        CreditAgreement creditAgreement = creditApplication.getCreditAgreement();

        if (creditAgreement == null) {
            throw new IllegalStateException("Credit agreement not found for application " + applicationId);
        }

        if (creditAgreement.getSigningStatus() == SigningStatus.SIGNED) {
            return creditApplicationMapper.fromCreditAgreement(creditAgreement);
        }

        creditAgreement.setSigningStatus(SigningStatus.SIGNED);
        creditAgreement.setSignedAt(LocalDateTime.now());

        CreditAgreementDto agreementDto = creditApplicationMapper.fromCreditAgreement(creditAgreement);

        return agreementDto;
    }

    @Transactional(readOnly = true)
    public PageResponseDto<CreditApplicationDto> getPageCreditApplications(@Valid @Positive int page) {
        return getPageResponse(
                () -> creditApplicationRepository.findAll(new Pageable(page, APPLICATION_PAGE_SIZE)),
                creditApplicationMapper::fromCreditApplication
        );
    }

    @Transactional(readOnly = true)
    public PageResponseDto<CreditAgreementDto> getPageCreditAgreements(@Valid @Positive int page) {
        return getPageResponse(
                () -> creditAgreementRepository.findAll(new Pageable(page, APPLICATION_PAGE_SIZE)),
                creditApplicationMapper::fromCreditAgreement
        );
    }
}
