package dev.sc.apm.service;

import dev.sc.apm.dto.*;
import dev.sc.apm.mapper.ClientMapper;
import dev.sc.apm.mapper.CreditApplicationMapper;
import dev.sc.apm.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

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
        return null;
    }

    @Transactional
    public CreditAgreementDto signCreditAgreement(@Valid @Positive long applicationId) {
        return null;
    }

    @Transactional(readOnly = true)
    public PageResponseDto<CreditApplicationDto> getPageCreditApplications(@Valid @Positive int page) {
        return null;
    }

    @Transactional(readOnly = true)
    public PageResponseDto<CreditAgreementDto> getPageCreditAgreements(@Valid @Positive int page) {
        return null;
    }
}
