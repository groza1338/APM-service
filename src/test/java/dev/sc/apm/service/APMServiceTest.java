package dev.sc.apm.service;

import dev.sc.apm.config.TestContainerConfig;
import dev.sc.apm.dto.CreditAgreementDto;
import dev.sc.apm.dto.CreditApplicantDto;
import dev.sc.apm.dto.CreditApplicationDto;
import dev.sc.apm.dto.CreditApplicationRequestDto;
import dev.sc.apm.entity.*;
import dev.sc.apm.exception.ClientInfoMismatchException;
import dev.sc.apm.exception.CreditApplicationNotFound;
import dev.sc.apm.exception.PendingStatusDuringSigningException;
import dev.sc.apm.exception.RejectedStatusDuringSigningException;
import dev.sc.apm.mapper.CreditApplicationMapper;
import dev.sc.apm.repository.ClientRepository;
import dev.sc.apm.repository.CreditAgreementRepository;
import dev.sc.apm.repository.CreditApplicationRepository;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;

import static dev.sc.apm.assertion.CustomAssert.*;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ContextConfiguration(initializers = TestContainerConfig.Initializer.class)
public class APMServiceTest {

    @SpyBean
    private CreditApplicationMapper creditApplicationMapper;
    @SpyBean
    private ClientRepository clientRepository;
    @SpyBean
    private CreditApplicationRepository creditApplicationRepository;
    @SpyBean
    private CreditAgreementRepository creditAgreementRepository;
    @SpyBean
    private AnalyticService analyticService;
    @Autowired
    private APMService apmService;

    private final static AtomicInteger passportNumber = new AtomicInteger(0);

    private String getNextPassport() {
        return String.format("%010d", passportNumber.getAndIncrement());
    }

    private BigDecimal amountApproved(BigDecimal requestedAmount) {
        return BigDecimal.valueOf(requestedAmount.doubleValue() * 0.9);
    }

    private final String phone1 = "+79991234567";
    private final String phone2 = "89991234568";

    @BeforeEach
    public void clear() {
        clientRepository.clearAll();
        creditApplicationRepository.clearAll();
        creditAgreementRepository.clearAll();
        passportNumber.set(0);
    }

    /*
     * Tests for APMService.createCreditApplication(...)
     *
     * Aspects of testing:
     * 1. Applicant status: new or existing;
     * 2. Count existing applications from the applicant;
     * 3. Result of application review: approved or rejected;
     * 4. Amount requested credit: small, big;
     * 5. Approved credit amount;
     * 6. Approved credit term;
     * 7. Invalid input data:
     *   - invalid applicant names: first name, last name, middle name;
     *   - invalid passport;
     *   - invalid phone number;
     *   - invalid organization name or/and position;
     *   - negative credit amount;
     *   - mismatch between existing applicant info and provided info;
     * */

    // Test 1 New applicant, new applications, approved
    // Expected: create client, create application, approved
    @Test
    public void createCreditApplicationByNewClient() {
        var request = CreditApplicationRequestDto.builder()
                .applicant(CreditApplicantDto.builder()
                        .firstName("Ivan")
                        .lastName("Ivanov")
                        .middleName("Ivanovich")
                        .passport(getNextPassport())
                        .phone(phone1)
                        .maritalStatus(MaritalStatus.MARRIED)
                        .address("Address")
                        .organizationName("Organization")
                        .position("Position")
                        .employmentPeriod(Duration.ofDays(365))
                        .build())
                .amount(BigDecimal.valueOf(10_000))
                .build();

        when(analyticService.approveCredit()).thenReturn(true);

        var actual = apmService.createCreditApplication(request);

        var expected = CreditApplicationDto.builder()
                .id(actual.getId())
                .status(CreditApplicationStatus.APPROVED)
                .approvedAmount(amountApproved(request.getAmount()))
                .requestedAmount(request.getAmount())
                .applicantId(actual.getApplicantId())
                .approvedTerm(actual.getApprovedTerm())
                .createdAt(actual.getCreatedAt())
                .creditAgreementId(actual.getCreditAgreementId())
                .build();

        assertEquals(expected, actual);

        var expectedStoredApplication = CreditApplication.builder()
                .id(actual.getId())
                .client(Client.builder()
                        .id(actual.getApplicantId())
                        .firstName(request.getApplicant().getFirstName())
                        .lastName(request.getApplicant().getLastName())
                        .middleName(request.getApplicant().getMiddleName())
                        .maritalStatus(request.getApplicant().getMaritalStatus())
                        .passport(request.getApplicant().getPassport())
                        .phone(request.getApplicant().getPhone())
                        .address(request.getApplicant().getAddress())
                        .organizationName(request.getApplicant().getOrganizationName())
                        .position(request.getApplicant().getPosition())
                        .employmentPeriod(request.getApplicant().getEmploymentPeriod())
                        .build()
                )
                .requestedAmount(request.getAmount())
                .status(CreditApplicationStatus.APPROVED)
                .approvedAmount(amountApproved(request.getAmount()))
                .approvedTerm(actual.getApprovedTerm())
                .createdAt(actual.getCreatedAt())
                .creditAgreement(CreditAgreement.builder()
                        .id(actual.getCreditAgreementId())
                        .signedAt(null)
                        .signingStatus(SigningStatus.NOT_SIGNED)
                        .build()
                )
                .build();

        expectedStoredApplication.getCreditAgreement().setApplication(expectedStoredApplication);

        var actualStoredApplication = creditApplicationRepository.findById(actual.getId()).orElse(null);

        assertEquals(actualStoredApplication.getClient().getId(), actual.getApplicantId());
        assertEqualsCreditApplication(expectedStoredApplication, actualStoredApplication);
    }

    // Test 2 existed applicant, new applications, approved
    // Expected: NOT create client, create application, approved
    @Test
    public void createCreditApplicationByExistedClient() {

        var client = Client.builder()
                .firstName("Ivan")
                .lastName("Ivanov")
                .middleName("Ivanovich")
                .passport(getNextPassport())
                .phone(phone2)
                .maritalStatus(MaritalStatus.MARRIED)
                .address("Address")
                .organizationName("Organization")
                .position("Position")
                .employmentPeriod(Duration.ofDays(200))
                .build();


        client = clientRepository.save(client);


        LocalDateTime createAt = LocalDateTime.now();

        var clientApplication1 = CreditApplication.builder()
                .client(client)
                .requestedAmount(BigDecimal.valueOf(10_000))
                .status(CreditApplicationStatus.APPROVED)
                .approvedAmount(amountApproved(BigDecimal.valueOf(10_000)))
                .approvedTerm(30)
                .createdAt(createAt)
                .creditAgreement(CreditAgreement.builder()
                        .signedAt(null)
                        .signingStatus(SigningStatus.NOT_SIGNED)
                        .build()
                )
                .build();

        var clientApplication2 = CreditApplication.builder()
                .client(client)
                .requestedAmount(BigDecimal.valueOf(100_000))
                .status(CreditApplicationStatus.REJECTED)
                .approvedAmount(amountApproved(BigDecimal.valueOf(100_000)))
                .createdAt(createAt)
                .build();

        creditApplicationRepository.save(clientApplication1);
        creditApplicationRepository.save(clientApplication2);

        var request = CreditApplicationRequestDto.builder()
                .applicant(CreditApplicantDto.builder()
                        .firstName(client.getFirstName())
                        .lastName(client.getLastName())
                        .middleName(client.getMiddleName())
                        .passport(client.getPassport())
                        .phone(client.getPhone())
                        .maritalStatus(client.getMaritalStatus())
                        .address(client.getAddress())
                        .organizationName(client.getOrganizationName())
                        .position(client.getPosition())
                        .employmentPeriod(client.getEmploymentPeriod())
                        .build())
                .amount(BigDecimal.valueOf(10_000))
                .build();

        when(analyticService.approveCredit()).thenReturn(true);

        long countBefore = creditApplicationRepository.count();

        var actual = apmService.createCreditApplication(request);

        long countAfter = creditApplicationRepository.count();

        assertEquals(countBefore, countAfter - 1);

        var expected = CreditApplicationDto.builder()
                .id(actual.getId())
                .status(CreditApplicationStatus.APPROVED)
                .approvedAmount(amountApproved(request.getAmount()))
                .requestedAmount(request.getAmount())
                .applicantId(client.getId())
                .approvedTerm(actual.getApprovedTerm())
                .createdAt(actual.getCreatedAt())
                .creditAgreementId(actual.getCreditAgreementId())
                .build();

        assertEquals(expected, actual);
        verify(clientRepository, times(1)).save(any()); // 1 - only for creation test env

        var expectedStoredApplication = CreditApplication.builder()
                .id(actual.getId())
                .client(client)
                .requestedAmount(request.getAmount())
                .status(CreditApplicationStatus.APPROVED)
                .approvedAmount(amountApproved(request.getAmount()))
                .approvedTerm(actual.getApprovedTerm())
                .createdAt(actual.getCreatedAt())
                .creditAgreement(CreditAgreement.builder()
                        .id(actual.getCreditAgreementId())
                        .signedAt(null)
                        .signingStatus(SigningStatus.NOT_SIGNED)
                        .build()
                )
                .build();

        expectedStoredApplication.getCreditAgreement().setApplication(expectedStoredApplication);

        var actualStoredApplication = creditApplicationRepository.findById(actual.getId()).orElse(null);

        assertEquals(actualStoredApplication.getClient().getId(), client.getId());
        assertEqualsCreditApplication(expectedStoredApplication, actualStoredApplication);
    }

    // Test 3 existing applicant, new applications, rejected
    // Expected: NOT create client, create application, rejected, not create agreement
    @Test
    public void createCreditApplicationByExistedClientRejected() {
        var client = Client.builder()
                .firstName("Ivan")
                .lastName("Ivanov")
                .middleName("Ivanovich")
                .passport(getNextPassport())
                .phone(phone2)
                .maritalStatus(MaritalStatus.MARRIED)
                .address("Address")
                .organizationName("Organization")
                .position("Position")
                .employmentPeriod(Duration.ofDays(200))
                .build();

        client = clientRepository.save(client);

        LocalDateTime createAt = LocalDateTime.now();

        var clientApplication1 = CreditApplication.builder()
                .client(client)
                .requestedAmount(BigDecimal.valueOf(10_000))
                .status(CreditApplicationStatus.APPROVED)
                .approvedAmount(amountApproved(BigDecimal.valueOf(10_000)))
                .approvedTerm(30)
                .createdAt(createAt)
                .creditAgreement(CreditAgreement.builder()
                        .signedAt(null)
                        .signingStatus(SigningStatus.NOT_SIGNED)
                        .build()
                )
                .build();

        var clientApplication2 = CreditApplication.builder()
                .client(client)
                .requestedAmount(BigDecimal.valueOf(100_000))
                .status(CreditApplicationStatus.REJECTED)
                .approvedAmount(amountApproved(BigDecimal.valueOf(100_000)))
                .createdAt(createAt)
                .build();

        creditApplicationRepository.save(clientApplication1);
        creditApplicationRepository.save(clientApplication2);

        var request = CreditApplicationRequestDto.builder()
                .applicant(CreditApplicantDto.builder()
                        .firstName(client.getFirstName())
                        .lastName(client.getLastName())
                        .middleName(client.getMiddleName())
                        .passport(client.getPassport())
                        .phone(client.getPhone())
                        .maritalStatus(client.getMaritalStatus())
                        .address(client.getAddress())
                        .organizationName(client.getOrganizationName())
                        .position(client.getPosition())
                        .employmentPeriod(client.getEmploymentPeriod())
                        .build())
                .amount(BigDecimal.valueOf(10_000))
                .build();

        when(analyticService.approveCredit()).thenReturn(false);

        long countApplicationsBefore = creditApplicationRepository.count();
        long countAgreementsBefore = creditAgreementRepository.count();

        var actual = apmService.createCreditApplication(request);

        long countApplicationsAfter = creditApplicationRepository.count();
        long countAgreementsAfter = creditAgreementRepository.count();

        verify(clientRepository, times(1)).save(any()); // 1 - only for creation test env
        verify(creditAgreementRepository, never()).save(any());
        assertEquals(countApplicationsBefore, countApplicationsAfter - 1);
        assertEquals(countAgreementsBefore, countAgreementsAfter);

        var expected = CreditApplicationDto.builder()
                .id(actual.getId())
                .status(CreditApplicationStatus.REJECTED)
                .requestedAmount(request.getAmount())
                .applicantId(client.getId())
                .createdAt(actual.getCreatedAt())
                .build();


        assertEquals(expected, actual);

        var expectedStoredApplication = CreditApplication.builder()
                .id(actual.getId())
                .client(client)
                .requestedAmount(request.getAmount())
                .status(CreditApplicationStatus.REJECTED)
                .approvedAmount(null)
                .approvedTerm(null)
                .createdAt(actual.getCreatedAt())
                .creditAgreement(null)
                .build();

        var actualStoredApplication = creditApplicationRepository.findById(actual.getId()).orElse(null);

        assertEquals(actualStoredApplication.getClient().getId(), client.getId());
        assertEqualsCreditApplication(expectedStoredApplication, actualStoredApplication);
    }

    // Test 4 new applicant, new applications, rejected
    // Expected: create client, create application, rejected, not create agreement
    @Test
    public void createCreditApplicationByNewClientRejected() {
        var request = CreditApplicationRequestDto.builder()
                .applicant(CreditApplicantDto.builder()
                        .firstName("Ivan")
                        .lastName("Ivanov")
                        .middleName("Ivanovich")
                        .passport(getNextPassport())
                        .phone(phone1)
                        .maritalStatus(MaritalStatus.MARRIED)
                        .address("Address")
                        .organizationName("Organization")
                        .position("Position")
                        .employmentPeriod(Duration.ofDays(365))
                        .build())
                .amount(BigDecimal.valueOf(10_000_000))
                .build();

        when(analyticService.approveCredit()).thenReturn(false);

        long countApplicationsBefore = creditApplicationRepository.count();
        long countAgreementsBefore = creditAgreementRepository.count();

        var actual = apmService.createCreditApplication(request);

        long countApplicationsAfter = creditApplicationRepository.count();
        long countAgreementsAfter = creditAgreementRepository.count();

        verify(creditAgreementRepository, never()).save(any());
        assertEquals(countApplicationsBefore, countApplicationsAfter - 1);
        assertEquals(countAgreementsBefore, countAgreementsAfter);

        var expected = CreditApplicationDto.builder()
                .id(actual.getId())
                .status(CreditApplicationStatus.REJECTED)
                .requestedAmount(request.getAmount())
                .applicantId(actual.getApplicantId())
                .createdAt(actual.getCreatedAt())
                .build();

        assertEquals(expected, actual);

        var expectedStoredApplication = CreditApplication.builder()
                .id(actual.getId())
                .client(Client.builder()
                        .id(actual.getApplicantId())
                        .firstName(request.getApplicant().getFirstName())
                        .lastName(request.getApplicant().getLastName())
                        .middleName(request.getApplicant().getMiddleName())
                        .maritalStatus(request.getApplicant().getMaritalStatus())
                        .passport(request.getApplicant().getPassport())
                        .phone(request.getApplicant().getPhone())
                        .address(request.getApplicant().getAddress())
                        .organizationName(request.getApplicant().getOrganizationName())
                        .position(request.getApplicant().getPosition())
                        .employmentPeriod(request.getApplicant().getEmploymentPeriod())
                        .build()
                )
                .requestedAmount(request.getAmount())
                .status(CreditApplicationStatus.REJECTED)
                .approvedAmount(null)
                .approvedTerm(null)
                .creditAgreement(null)
                .createdAt(actual.getCreatedAt())
                .build();

        var actualStoredApplication = creditApplicationRepository.findById(actual.getId()).orElse(null);

        assertEquals(actualStoredApplication.getClient().getId(), actual.getApplicantId());
        assertEqualsCreditApplication(expectedStoredApplication, actualStoredApplication);
    }

    // Test 5.1 mismatch between existing applicant info and provided info(by names)
    // Expected: throw ClientInfoMismatchException
    @Test
    public void createCreditApplicationMismatch() {
        var client = Client.builder()
                .firstName("Alex")
                .lastName("Andreev")
                .passport(getNextPassport())
                .phone(phone1)
                .maritalStatus(MaritalStatus.MARRIED)
                .address("Address")
                .organizationName("Organization")
                .position("Position")
                .employmentPeriod(Duration.ofDays(200))
                .build();

        client = clientRepository.save(client);

        var request = CreditApplicationRequestDto.builder()
                .applicant(CreditApplicantDto.builder()
                        .firstName("Ivan")
                        .lastName("Ivanov")
                        .middleName("Ivanovich")
                        .passport(client.getPassport())
                        .phone(phone1)
                        .maritalStatus(MaritalStatus.MARRIED)
                        .address("Address")
                        .organizationName("Organization")
                        .position("Position")
                        .employmentPeriod(Duration.ofDays(200))
                        .build())
                .amount(BigDecimal.valueOf(10_000))
                .build();

        assertThrows(ClientInfoMismatchException.class, () -> apmService.createCreditApplication(request));
    }

    // Test 5.2 mismatch between existing applicant info and provided info(by phone)
    // Expected: throw ClientInfoMismatchException
    @Test
    public void createCreditApplicationMismatchPhone() {
        var client = Client.builder()
                .firstName("Ivan")
                .lastName("Ivanov")
                .passport(getNextPassport())
                .phone(phone1)
                .maritalStatus(MaritalStatus.MARRIED)
                .address("Address")
                .organizationName("Organization")
                .position("Position")
                .employmentPeriod(Duration.ofDays(200))
                .build();

        client = clientRepository.save(client);

        var request = CreditApplicationRequestDto.builder()
                .applicant(CreditApplicantDto.builder()
                        .firstName(client.getFirstName())
                        .lastName(client.getLastName())
                        .middleName(client.getMiddleName())
                        .passport(client.getPassport())
                        .phone(phone2)
                        .maritalStatus(client.getMaritalStatus())
                        .address(client.getAddress())
                        .organizationName(client.getOrganizationName())
                        .position(client.getPosition())
                        .employmentPeriod(client.getEmploymentPeriod())
                        .build())
                .amount(BigDecimal.valueOf(10_000))
                .build();

        assertThrows(ClientInfoMismatchException.class, () -> apmService.createCreditApplication(request));
    }

    // Test 5.3 mismatch between existing applicant info and provided info(by employment)
    // Expected: throw ClientInfoMismatchException
    @Test
    public void createCreditApplicationMismatchEmployment() {
        var client = Client.builder()
                .firstName("Ivan")
                .lastName("Ivanov")
                .passport(getNextPassport())
                .phone(phone1)
                .maritalStatus(MaritalStatus.MARRIED)
                .address("Address")
                .organizationName("Yandex")
                .position("Position")
                .employmentPeriod(Duration.ofDays(200))
                .build();

        client = clientRepository.save(client);

        var request = CreditApplicationRequestDto.builder()
                .applicant(CreditApplicantDto.builder()
                        .firstName(client.getFirstName())
                        .lastName(client.getLastName())
                        .middleName(client.getMiddleName())
                        .passport(client.getPassport())
                        .phone(client.getPhone())
                        .maritalStatus(client.getMaritalStatus())
                        .address(client.getAddress())
                        .organizationName("Meta*")
                        .position(client.getPosition())
                        .employmentPeriod(Duration.ofDays(365))
                        .build())
                .amount(BigDecimal.valueOf(10_000))
                .build();

        assertThrows(ClientInfoMismatchException.class, () -> apmService.createCreditApplication(request));
    }

    // Test 6.1 invalid first name
    // Expected: throw ConstraintViolationException
    @Test
    public void createCreditApplicationInvalidFirstName() {
        var request = CreditApplicationRequestDto.builder()
                .applicant(CreditApplicantDto.builder()
                        .firstName("3267")
                        .lastName("Ivanov")
                        .middleName("Ivanovich")
                        .passport(getNextPassport())
                        .phone(phone1)
                        .maritalStatus(MaritalStatus.MARRIED)
                        .address("Address")
                        .organizationName("Organization")
                        .position("Position")
                        .employmentPeriod(Duration.ofDays(365))
                        .build())
                .amount(BigDecimal.valueOf(10_000))
                .build();

        assertThrows(ConstraintViolationException.class, () -> apmService.createCreditApplication(request));
    }

    // Test 6.2 invalid last name
    // Expected: throw ConstraintViolationException
    @Test
    public void createCreditApplicationInvalidLastName() {
        var request = CreditApplicationRequestDto.builder()
                .applicant(CreditApplicantDto.builder()
                        .firstName("Ivan")
                        .lastName("Lex324-3")
                        .passport(getNextPassport())
                        .phone(phone1)
                        .maritalStatus(MaritalStatus.MARRIED)
                        .address("Address")
                        .organizationName("Organization")
                        .position("Position")
                        .employmentPeriod(Duration.ofDays(365))
                        .build())
                .amount(BigDecimal.valueOf(10_000))
                .build();

        assertThrows(ConstraintViolationException.class, () -> apmService.createCreditApplication(request));
    }

    // Test 6.3 invalid middle name
    // Expected: throw ConstraintViolationException
    @Test
    public void createCreditApplicationInvalidMiddleName() {
        var request = CreditApplicationRequestDto.builder()
                .applicant(CreditApplicantDto.builder()
                        .firstName("Emilia Daria")
                        .lastName("Kuznetsova")
                        .middleName("ndsfoi-u-98    q@")
                        .passport(getNextPassport())
                        .phone(phone1)
                        .maritalStatus(MaritalStatus.MARRIED)
                        .address("Address")
                        .organizationName("Organization")
                        .position("Position")
                        .employmentPeriod(Duration.ofDays(365))
                        .build())
                .amount(BigDecimal.valueOf(10_000))
                .build();

        assertThrows(ConstraintViolationException.class, () -> apmService.createCreditApplication(request));
    }

    // Test 6.4 invalid passport
    // Expected: throw ConstraintViolationException
    @Test
    public void createCreditApplicationInvalidPassport() {
        var request = CreditApplicationRequestDto.builder()
                .applicant(CreditApplicantDto.builder()
                        .firstName("Ivan")
                        .lastName("Ivanov")
                        .middleName("Ivanovich")
                        .passport("1234567890-")
                        .phone(phone1)
                        .maritalStatus(MaritalStatus.MARRIED)
                        .address("Address")
                        .organizationName("Organization")
                        .position("Position")
                        .employmentPeriod(Duration.ofDays(365))
                        .build())
                .amount(BigDecimal.valueOf(10_000))
                .build();

        assertThrows(ConstraintViolationException.class, () -> apmService.createCreditApplication(request));
    }

    // Test 6.4.1 invalid passport by length(less)
    // Expected: throw ConstraintViolationException
    @Test
    public void createCreditApplicationInvalidPassportLengthLess() {
        var request = CreditApplicationRequestDto.builder()
                .applicant(CreditApplicantDto.builder()
                        .firstName("Ivan")
                        .lastName("Ivanov")
                        .middleName("Ivanovich")
                        .passport("0")
                        .phone(phone1)
                        .maritalStatus(MaritalStatus.MARRIED)
                        .address("Address")
                        .organizationName("Organization")
                        .position("Position")
                        .employmentPeriod(Duration.ofDays(365))
                        .build())
                .amount(BigDecimal.valueOf(10_000))
                .build();

        assertThrows(ConstraintViolationException.class, () -> apmService.createCreditApplication(request));
    }

    // Test 6.4.2 invalid passport by length(more)
    // Expected: throw ConstraintViolationException
    @Test
    public void createCreditApplicationInvalidPassportLengthMore() {
        var request = CreditApplicationRequestDto.builder()
                .applicant(CreditApplicantDto.builder()
                        .firstName("Emilia Daria")
                        .lastName("Kuznetsova")
                        .middleName("Ivanovich")
                        .passport("12345678901")
                        .phone(phone1)
                        .maritalStatus(MaritalStatus.MARRIED)
                        .address("Address")
                        .organizationName("Organization")
                        .position("Position")
                        .employmentPeriod(Duration.ofDays(365))
                        .build())
                .amount(BigDecimal.valueOf(10_000))
                .build();

        assertThrows(ConstraintViolationException.class, () -> apmService.createCreditApplication(request));
    }

    // Test 6.5 invalid phone
    // Expected: throw ConstraintViolationException
    @Test
    public void createCreditApplicationInvalidPhone() {
        var request = CreditApplicationRequestDto.builder()
                .applicant(CreditApplicantDto.builder()
                        .firstName("Ivan")
                        .lastName("Ivanov")
                        .middleName("Ivanovich")
                        .passport(getNextPassport())
                        .phone("+1(234)-567-89-00")
                        .maritalStatus(MaritalStatus.MARRIED)
                        .address("Address")
                        .organizationName("Organization")
                        .position("Position")
                        .employmentPeriod(Duration.ofDays(365))
                        .build())
                .amount(BigDecimal.valueOf(10_000))
                .build();

        assertThrows(ConstraintViolationException.class, () -> apmService.createCreditApplication(request));
    }

    // Test 6.5.1 invalid phone by length(less)
    // Expected: throw ConstraintViolationException
    @Test
    public void createCreditApplicationInvalidPhoneLengthLess() {
        var request = CreditApplicationRequestDto.builder()
                .applicant(CreditApplicantDto.builder()
                        .firstName("Emilia Daria")
                        .lastName("Kuznetsova")
                        .middleName("Ivanovich")
                        .passport(getNextPassport())
                        .phone("+7999123")
                        .maritalStatus(MaritalStatus.MARRIED)
                        .address("Address")
                        .organizationName("Organization")
                        .position("Position")
                        .employmentPeriod(Duration.ofDays(365))
                        .build())
                .amount(BigDecimal.valueOf(10_000))
                .build();

        assertThrows(ConstraintViolationException.class, () -> apmService.createCreditApplication(request));
    }

    // Test 6.5.2 invalid phone by length(more)
    // Expected: throw ConstraintViolationException
    @Test
    public void createCreditApplicationInvalidPhoneLengthMore() {
        var request = CreditApplicationRequestDto.builder()
                .applicant(CreditApplicantDto.builder()
                        .firstName("Emilia Daria")
                        .lastName("Kuznetsova")
                        .middleName("Ivanovich")
                        .passport(getNextPassport())
                        .phone("+7999123456789")
                        .maritalStatus(MaritalStatus.MARRIED)
                        .address("Address")
                        .organizationName("Organization")
                        .position("Position")
                        .employmentPeriod(Duration.ofDays(365))
                        .build())
                .amount(BigDecimal.valueOf(10_000))
                .build();

        assertThrows(ConstraintViolationException.class, () -> apmService.createCreditApplication(request));
    }

    // Test 6.6 invalid organization name(null)
    // Expected: throw ConstraintViolationException
    @Test
    public void createCreditApplicationInvalidOrganizationNameNull() {
        var request = CreditApplicationRequestDto.builder()
                .applicant(CreditApplicantDto.builder()
                        .firstName("Emilia Daria")
                        .lastName("Kuznetsova")
                        .middleName("Ivanovich")
                        .passport(getNextPassport())
                        .phone(phone1)
                        .maritalStatus(MaritalStatus.MARRIED)
                        .address("Address")
                        .organizationName(null)
                        .position("Position")
                        .employmentPeriod(Duration.ofDays(365))
                        .build())
                .amount(BigDecimal.valueOf(10_000))
                .build();

        assertThrows(ConstraintViolationException.class, () -> apmService.createCreditApplication(request));
    }

    // Test 6.6.1 invalid organization name(empty)
    // Expected: throw ConstraintViolationException
    @Test
    public void createCreditApplicationInvalidOrganizationNameEmpty() {
        var request = CreditApplicationRequestDto.builder()
                .applicant(CreditApplicantDto.builder()
                        .firstName("Emilia Daria")
                        .lastName("Kuznetsova")
                        .middleName("Ivanovich")
                        .passport(getNextPassport())
                        .phone(phone1)
                        .maritalStatus(MaritalStatus.MARRIED)
                        .address("Address")
                        .organizationName("")
                        .position("Position")
                        .employmentPeriod(Duration.ofDays(365))
                        .build())
                .amount(BigDecimal.valueOf(10_000))
                .build();

        assertThrows(ConstraintViolationException.class, () -> apmService.createCreditApplication(request));
    }

    // Test 6.6.2 invalid organization name(whitespace)
    // Expected: throw ConstraintViolationException
    @Test
    public void createCreditApplicationInvalidOrganizationNameWhitespace() {
        var request = CreditApplicationRequestDto.builder()
                .applicant(CreditApplicantDto.builder()
                        .firstName("Emilia Daria")
                        .lastName("Kuznetsova")
                        .middleName("Ivanovich")
                        .passport(getNextPassport())
                        .phone(phone1)
                        .maritalStatus(MaritalStatus.MARRIED)
                        .address("Address")
                        .organizationName("  ")
                        .position("Position")
                        .employmentPeriod(Duration.ofDays(365))
                        .build())
                .amount(BigDecimal.valueOf(10_000))
                .build();

        assertThrows(ConstraintViolationException.class, () -> apmService.createCreditApplication(request));
    }

    // Test 6.7 invalid position(null)
    // Expected: throw ConstraintViolationException
    @Test
    public void createCreditApplicationInvalidPositionNull() {
        var request = CreditApplicationRequestDto.builder()
                .applicant(CreditApplicantDto.builder()
                        .firstName("Emilia Daria")
                        .lastName("Kuznetsova")
                        .middleName("Ivanovich")
                        .passport(getNextPassport())
                        .phone(phone1)
                        .maritalStatus(MaritalStatus.MARRIED)
                        .address("Address")
                        .organizationName("Organization")
                        .position(null)
                        .employmentPeriod(Duration.ofDays(365))
                        .build())
                .amount(BigDecimal.valueOf(10_000))
                .build();

        assertThrows(ConstraintViolationException.class, () -> apmService.createCreditApplication(request));
    }

    // Test 6.7.1 invalid position(empty)
    // Expected: throw ConstraintViolationException
    @Test
    public void createCreditApplicationInvalidPositionEmpty() {
        var request = CreditApplicationRequestDto.builder()
                .applicant(CreditApplicantDto.builder()
                        .firstName("Emilia Daria")
                        .lastName("Kuznetsova")
                        .middleName("Ivanovich")
                        .passport(getNextPassport())
                        .phone(phone1)
                        .maritalStatus(MaritalStatus.MARRIED)
                        .address("Address")
                        .organizationName("Organization")
                        .position("")
                        .employmentPeriod(Duration.ofDays(365))
                        .build())
                .amount(BigDecimal.valueOf(10_000))
                .build();

        assertThrows(ConstraintViolationException.class, () -> apmService.createCreditApplication(request));
    }

    // Test 6.7.2 invalid position(whitespace)
    // Expected: throw ConstraintViolationException
    @Test
    public void createCreditApplicationInvalidPositionWhitespace() {
        var request = CreditApplicationRequestDto.builder()
                .applicant(CreditApplicantDto.builder()
                        .firstName("Emilia Daria")
                        .lastName("Kuznetsova")
                        .middleName("Ivanovich")
                        .passport(getNextPassport())
                        .phone(phone1)
                        .maritalStatus(MaritalStatus.MARRIED)
                        .address("Address")
                        .organizationName("Organization")
                        .position("  ")
                        .employmentPeriod(Duration.ofDays(365))
                        .build())
                .amount(BigDecimal.valueOf(10_000))
                .build();

        assertThrows(ConstraintViolationException.class, () -> apmService.createCreditApplication(request));
    }

    // Test 6.8 invalid employment period(null)
    // Expected: throw ConstraintViolationException
    @Test
    public void createCreditApplicationInvalidEmploymentPeriodNull() {
        var request = CreditApplicationRequestDto.builder()
                .applicant(CreditApplicantDto.builder()
                        .firstName("Emilia Daria")
                        .lastName("Kuznetsova")
                        .middleName("Ivanovich")
                        .passport(getNextPassport())
                        .phone(phone1)
                        .maritalStatus(MaritalStatus.MARRIED)
                        .address("Address")
                        .organizationName("Organization")
                        .position("Position")
                        .employmentPeriod(null)
                        .build())
                .amount(BigDecimal.valueOf(10_000))
                .build();

        assertThrows(ConstraintViolationException.class, () -> apmService.createCreditApplication(request));
    }

    // Test 6.9 invalid amount(negative)
    // Expected: throw ConstraintViolationException
    @Test
    public void createCreditApplicationInvalidAmountNegative() {
        var request = CreditApplicationRequestDto.builder()
                .applicant(CreditApplicantDto.builder()
                        .firstName("Emilia Daria")
                        .lastName("Kuznetsova")
                        .middleName("Ivanovich")
                        .passport(getNextPassport())
                        .phone(phone1)
                        .maritalStatus(MaritalStatus.MARRIED)
                        .address("Address")
                        .organizationName("Organization")
                        .position("Position")
                        .employmentPeriod(Duration.ofDays(365))
                        .build())
                .amount(BigDecimal.valueOf(-10_000))
                .build();

        assertThrows(ConstraintViolationException.class, () -> apmService.createCreditApplication(request));
    }

    // Test 6.10 invalid amount(null)
    // Expected: throw ConstraintViolationException
    @Test
    public void createCreditApplicationInvalidAmountNull() {
        var request = CreditApplicationRequestDto.builder()
                .applicant(CreditApplicantDto.builder()
                        .firstName("Emilia Daria")
                        .lastName("Kuznetsova")
                        .middleName("Ivanovich")
                        .passport(getNextPassport())
                        .phone(phone1)
                        .maritalStatus(MaritalStatus.MARRIED)
                        .address("Address")
                        .organizationName("Organization")
                        .position("Position")
                        .employmentPeriod(Duration.ofDays(365))
                        .build())
                .amount(null)
                .build();

        assertThrows(ConstraintViolationException.class, () -> apmService.createCreditApplication(request));
    }

    // Test 6.11 invalid amount(0)
    // Expected: throw ConstraintViolationException
    @Test
    public void createCreditApplicationInvalidAmountZero() {
        var request = CreditApplicationRequestDto.builder()
                .applicant(CreditApplicantDto.builder()
                        .firstName("Emilia Daria")
                        .lastName("Kuznetsova")
                        .middleName("Ivanovich")
                        .passport(getNextPassport())
                        .phone(phone1)
                        .maritalStatus(MaritalStatus.MARRIED)
                        .address("Address")
                        .organizationName("Organization")
                        .position("Position")
                        .employmentPeriod(Duration.ofDays(365))
                        .build())
                .amount(BigDecimal.ZERO)
                .build();

        assertThrows(ConstraintViolationException.class, () -> apmService.createCreditApplication(request));
    }

    // Test 6.12 invalid amount(less than 1)
    // Expected: throw ConstraintViolationException
    @Test
    public void createCreditApplicationInvalidAmountLessThanOne() {
        var request = CreditApplicationRequestDto.builder()
                .applicant(CreditApplicantDto.builder()
                        .firstName("Emilia Daria")
                        .lastName("Kuznetsova")
                        .middleName("Ivanovich")
                        .passport(getNextPassport())
                        .phone(phone1)
                        .maritalStatus(MaritalStatus.MARRIED)
                        .address("Address")
                        .organizationName("Organization")
                        .position("Position")
                        .employmentPeriod(Duration.ofDays(365))
                        .build())
                .amount(BigDecimal.valueOf(0.5))
                .build();

        assertThrows(ConstraintViolationException.class, () -> apmService.createCreditApplication(request));
    }

    /*
     * Tests for APMService.signCreditAgreement
     * Aspects of testing:
     * 1. Application status: approved, rejected, pending, already signed;
     * 2. Application existing: exists, not exists;
     * 3. Agreement status: signed, not signed;
     * 4. Invalid input data:
     *     - application id is negative or zero
     * */

    // Test 1.1 sign approved application
    // Expected: sign agreement
    @Test
    public void signCreditAgreementApproved() {
        var client = Client.builder()
                .firstName("Ivan")
                .lastName("Ivanov")
                .passport(getNextPassport())
                .phone(phone1)
                .maritalStatus(MaritalStatus.MARRIED)
                .address("Address")
                .organizationName("Organization")
                .position("Position")
                .employmentPeriod(Duration.ofDays(200))
                .build();

        client = clientRepository.save(client);

        LocalDateTime createAt = LocalDateTime.now();

        var clientApplication = CreditApplication.builder()
                .client(client)
                .requestedAmount(BigDecimal.valueOf(10_000))
                .status(CreditApplicationStatus.APPROVED)
                .approvedAmount(amountApproved(BigDecimal.valueOf(10_000)))
                .approvedTerm(30)
                .createdAt(createAt)
                .creditAgreement(null)
                .build();

        clientApplication = creditApplicationRepository.save(clientApplication);

        var clientApplicationAgreement = CreditAgreement.builder()
                .application(clientApplication)
                .signedAt(null)
                .signingStatus(SigningStatus.NOT_SIGNED)
                .build();

        clientApplicationAgreement = creditAgreementRepository.save(clientApplicationAgreement);

        var actual = apmService.signCreditAgreement(clientApplication.getId());

        var expected = CreditAgreementDto.builder()
                .id(clientApplicationAgreement.getId())
                .applicationId(clientApplication.getId())
                .signedAt(actual.getSignedAt())
                .signingStatus(SigningStatus.SIGNED)
                .build();

        assertEquals(expected, actual);

        var expectedStoredAgreement = CreditAgreement.builder()
                .id(clientApplicationAgreement.getId())
                .application(clientApplication)
                .signedAt(actual.getSignedAt())
                .signingStatus(SigningStatus.SIGNED)
                .build();

        var actualStoredApplication = creditAgreementRepository.findById(actual.getId()).orElse(null);

        assertEqualsCreditAgreement(expectedStoredAgreement, actualStoredApplication);
    }

    // Test 1.2 sign rejected application
    // Expected: throw RejectedStatusDuringSigningException
    @Test
    public void signCreditAgreementRejected() {
        var client = Client.builder()
                .firstName("Ivan")
                .lastName("Ivanov")
                .passport(getNextPassport())
                .phone(phone1)
                .maritalStatus(MaritalStatus.MARRIED)
                .address("Address")
                .organizationName("Organization")
                .position("Position")
                .employmentPeriod(Duration.ofDays(200))
                .build();

        client = clientRepository.save(client);

        LocalDateTime createAt = LocalDateTime.now();

        var clientApplication = CreditApplication.builder()
                .client(client)
                .requestedAmount(BigDecimal.valueOf(10_000))
                .status(CreditApplicationStatus.REJECTED)
                .approvedAmount(null)
                .approvedTerm(null)
                .createdAt(createAt)
                .creditAgreement(null)
                .build();

        clientApplication = creditApplicationRepository.save(clientApplication);

        final long id = clientApplication.getId();
        assertThrows(RejectedStatusDuringSigningException.class, () -> apmService.signCreditAgreement(id));
    }

    // Test 1.3 sign pending application
    // Expected: throw PendingStatusDuringSigningException
    @Test
    public void signCreditAgreementPending() {
        var client = Client.builder()
                .firstName("Ivan")
                .lastName("Ivanov")
                .passport(getNextPassport())
                .phone(phone1)
                .maritalStatus(MaritalStatus.MARRIED)
                .address("Address")
                .organizationName("Organization")
                .position("Position")
                .employmentPeriod(Duration.ofDays(200))
                .build();

        client = clientRepository.save(client);

        LocalDateTime createAt = LocalDateTime.now();

        var clientApplication = CreditApplication.builder()
                .client(client)
                .requestedAmount(BigDecimal.valueOf(10_000))
                .status(CreditApplicationStatus.PENDING)
                .approvedAmount(null)
                .approvedTerm(null)
                .createdAt(createAt)
                .creditAgreement(null)
                .build();

        clientApplication = creditApplicationRepository.save(clientApplication);

        final long id = clientApplication.getId();
        assertThrows(PendingStatusDuringSigningException.class, () -> apmService.signCreditAgreement(id));
    }

    // Test 1.4 sign already signed application
    // Expected: return already signed application
    @Test
    public void signCreditAgreementAlreadySigned() {
        var client = Client.builder()
                .firstName("Ivan")
                .lastName("Ivanov")
                .passport(getNextPassport())
                .phone(phone1)
                .maritalStatus(MaritalStatus.MARRIED)
                .address("Address")
                .organizationName("Organization")
                .position("Position")
                .employmentPeriod(Duration.ofDays(200))
                .build();

        client = clientRepository.save(client);

        LocalDateTime createAt = LocalDateTime.now();

        var clientApplication = CreditApplication.builder()
                .client(client)
                .requestedAmount(BigDecimal.valueOf(10_000))
                .status(CreditApplicationStatus.APPROVED)
                .approvedAmount(amountApproved(BigDecimal.valueOf(10_000)))
                .approvedTerm(30)
                .createdAt(createAt)
                .creditAgreement(null)
                .build();

        clientApplication = creditApplicationRepository.save(clientApplication);

        var clientApplicationAgreement = CreditAgreement.builder()
                .application(clientApplication)
                .signedAt(LocalDateTime.now())
                .signingStatus(SigningStatus.SIGNED)
                .build();

        clientApplicationAgreement = creditAgreementRepository.save(clientApplicationAgreement);

        final long id = clientApplication.getId();
        var actual = apmService.signCreditAgreement(id);

        assertEqualsLocalDateTime(clientApplicationAgreement.getSignedAt(), actual.getSignedAt());

        var expected = CreditAgreementDto.builder()
                .id(clientApplicationAgreement.getId())
                .applicationId(clientApplication.getId())
                .signedAt(actual.getSignedAt())
                .signingStatus(SigningStatus.SIGNED)
                .build();

        assertEquals(expected, actual);

        var actualStoredApplication = creditAgreementRepository.findById(actual.getId()).orElse(null);

        assertEqualsCreditAgreement(clientApplicationAgreement, actualStoredApplication);
    }


    // Test 2.1 invalid application id(negative)
    // Expected: throw ConstraintViolationException
    @Test
    public void signCreditAgreementByNegativeId() {
        final long id = -1L;
        assertThrows(ConstraintViolationException.class, () -> apmService.signCreditAgreement(id));
    }

    // Test 2.2 invalid application id
    // Expected: throw ConstraintViolationException
    @Test
    public void signCreditAgreementByZeroId() {
        final long id = 0L;
        assertThrows(ConstraintViolationException.class, () -> apmService.signCreditAgreement(id));
    }

    // Test 3 sign not existing application
    // Expected: throw CreditApplicationNotFoundException
    @Test
    public void signCreditAgreementNotExistingApplication() {
        final long id = 101010101010101010L;
        assertThrows(CreditApplicationNotFound.class, () -> apmService.signCreditAgreement(id));
    }

    /*
     * Tests for APMService.getPageCreditApplications(...)
     * Aspects of testing:
     * 1. Count stored applications: 0, 1, several
     * 2. final page size: less than standard(currently standard size: 10), equals standard
     * 3. final page number: less than requested, equals requested
     * 4. Invalid input data:
     *    - page number is negative or zero
     * */

    // Test 1.1 get page consisted from several applications, page size equals standard, page number equals requested
    // Expected: return page with several applications, page size equals standard
    @Test
    public void getPageCreditApplicationsSeveralApplicationsPageSizeEqualsStandardPageNumberEqualsRequested() {
        final int totalApplications = 15;
        final int standardPageSize = 10;
        final int pageNumber = 1;

        int expectedPageSize = standardPageSize;
        int expectedTotal = totalApplications;
        int expectedPage = pageNumber;

        var client = Client.builder()
                .firstName("Ivan")
                .lastName("Ivanov")
                .passport(getNextPassport())
                .phone(phone1)
                .maritalStatus(MaritalStatus.MARRIED)
                .address("Address")
                .organizationName("Organization")
                .position("Position")
                .employmentPeriod(Duration.ofDays(200))
                .build();

        client = clientRepository.save(client);

        List<CreditApplicationDto> storedApplicationDtos = new ArrayList<>();

        for (int i = 0; i < totalApplications; i++) {
            var clientApplication = CreditApplication.builder()
                    .client(client)
                    .requestedAmount(BigDecimal.valueOf(10_000))
                    .status(CreditApplicationStatus.PENDING)
                    .approvedAmount(amountApproved(BigDecimal.valueOf(10_000)))
                    .approvedTerm(30)
                    .createdAt(LocalDateTime.now())
                    .creditAgreement(null)
                    .build();

            var saved = creditApplicationRepository.save(clientApplication);
            var savedDto = creditApplicationMapper.fromCreditApplication(saved);
            storedApplicationDtos.add(savedDto);
        }

        var actual = apmService.getPageCreditApplications(pageNumber);

        assertEquals(expectedPage, actual.getPage());
        assertEquals(expectedPageSize, actual.getPageSize());
        assertEquals(expectedTotal, actual.getTotal());
        assertEquals(expectedPageSize, actual.getContent().size());

        assertEquals(
                storedApplicationDtos.subList(0, expectedPageSize).stream().map(CreditApplicationDto::getId).toList(),
                actual.getContent().stream().map(CreditApplicationDto::getId).toList()
        );
    }

    // Test 1.2 get page consisted from several applications, page size less than standard, page number equals requested
    // Expected: return page with several applications, page size less than standard
    @Test
    public void getPageCreditApplicationsSeveralApplicationsPageSizeLessThanStandardPageNumberEqualsRequested() {
        final int totalApplications = 15;
        final int standardPageSize = 10;
        final int pageNumber = 2;

        int expectedPageSize = 5;
        int expectedTotal = totalApplications;
        int expectedPage = pageNumber;

        var client = Client.builder()
                .firstName("Ivan")
                .lastName("Ivanov")
                .passport(getNextPassport())
                .phone(phone1)
                .maritalStatus(MaritalStatus.MARRIED)
                .address("Address")
                .organizationName("Organization")
                .position("Position")
                .employmentPeriod(Duration.ofDays(200))
                .build();

        client = clientRepository.save(client);

        List<CreditApplicationDto> storedApplicationDtos = new ArrayList<>();

        for (int i = 0; i < totalApplications; i++) {
            var clientApplication = CreditApplication.builder()
                    .client(client)
                    .requestedAmount(BigDecimal.valueOf(10_000))
                    .status(CreditApplicationStatus.PENDING)
                    .approvedAmount(amountApproved(BigDecimal.valueOf(10_000)))
                    .approvedTerm(30)
                    .createdAt(LocalDateTime.now())
                    .creditAgreement(null)
                    .build();

            var saved = creditApplicationRepository.save(clientApplication);
            var savedDto = creditApplicationMapper.fromCreditApplication(saved);
            storedApplicationDtos.add(savedDto);
        }

        var actual = apmService.getPageCreditApplications(pageNumber);

        assertEquals(expectedPage, actual.getPage());
        assertEquals(expectedPageSize, actual.getPageSize());
        assertEquals(expectedTotal, actual.getTotal());
        assertEquals(expectedPageSize, actual.getContent().size());

        assertEquals(
                storedApplicationDtos.subList(standardPageSize, totalApplications).stream().map(CreditApplicationDto::getId).toList(),
                actual.getContent().stream().map(CreditApplicationDto::getId).toList()
        );
    }

    // Test 1.3 get page consisted from one application, page size less than standard, page number less than requested
    // Expected: return page with one application, page size less than standard
    @Test
    public void getPageCreditApplicationsOneApplicationPageSizeLessThanStandardPageNumberLessThanRequested() {
        final int totalApplications = 1;
        final int standardPageSize = 10;
        final int pageNumber = 2;

        int expectedPageSize = 1;
        int expectedTotal = totalApplications;
        int expectedPage = 1;

        var client = Client.builder()
                .firstName("Ivan")
                .lastName("Ivanov")
                .passport(getNextPassport())
                .phone(phone1)
                .maritalStatus(MaritalStatus.MARRIED)
                .address("Address")
                .organizationName("Organization")
                .position("Position")
                .employmentPeriod(Duration.ofDays(200))
                .build();

        client = clientRepository.save(client);

        List<CreditApplicationDto> storedApplicationDtos = new ArrayList<>();

        for (int i = 0; i < totalApplications; i++) {
            var clientApplication = CreditApplication.builder()
                    .client(client)
                    .requestedAmount(BigDecimal.valueOf(10_000))
                    .status(CreditApplicationStatus.PENDING)
                    .approvedAmount(amountApproved(BigDecimal.valueOf(10_000)))
                    .approvedTerm(30)
                    .createdAt(LocalDateTime.now())
                    .creditAgreement(null)
                    .build();

            var saved = creditApplicationRepository.save(clientApplication);
            var savedDto = creditApplicationMapper.fromCreditApplication(saved);
            storedApplicationDtos.add(savedDto);
        }

        var actual = apmService.getPageCreditApplications(pageNumber);

        assertEquals(expectedPage, actual.getPage());
        assertEquals(expectedPageSize, actual.getPageSize());
        assertEquals(expectedTotal, actual.getTotal());
        assertEquals(expectedPageSize, actual.getContent().size());

        assertEquals(
                storedApplicationDtos.stream().map(CreditApplicationDto::getId).toList(),
                actual.getContent().stream().map(CreditApplicationDto::getId).toList()
        );
    }

    // Test 1.4 get page consisted from zero applications, page size less than standard, page number less than requested
    // Expected: return page with zero applications, page size less than standard
    @Test
    public void getPageCreditApplicationsZeroApplicationsPageSizeLessThanStandardPageNumberLessThanRequested() {
        final int totalApplications = 0;
        final int standardPageSize = 10;
        final int pageNumber = 5;

        int expectedPageSize = 0;
        int expectedTotal = totalApplications;
        int expectedPage = 1;

        List<CreditApplicationDto> storedApplicationDtos = new ArrayList<>();

        var actual = apmService.getPageCreditApplications(pageNumber);

        assertEquals(expectedPage, actual.getPage());
        assertEquals(expectedPageSize, actual.getPageSize());
        assertEquals(expectedTotal, actual.getTotal());
        assertEquals(expectedPageSize, actual.getContent().size());

        assertEquals(
                storedApplicationDtos.stream().map(CreditApplicationDto::getId).toList(),
                actual.getContent().stream().map(CreditApplicationDto::getId).toList()
        );
    }

    // Test 2.1 invalid page number(negative)
    // Expected: throw ConstraintViolationException
    @Test
    public void getPageCreditApplicationsByNegativePageNumber() {
        final int pageNumber = -1;
        assertThrows(ConstraintViolationException.class, () -> apmService.getPageCreditApplications(pageNumber));
    }

    // Test 2.2 invalid page number(zero)
    // Expected: throw ConstraintViolationException
    @Test
    public void getPageCreditApplicationsByZeroPageNumber() {
        final int pageNumber = 0;
        assertThrows(ConstraintViolationException.class, () -> apmService.getPageCreditApplications(pageNumber));
    }

    /*
     * Tests for APMService.getPageCreditAgreements(...)
     * Aspects of testing:
     * 1. Count stored agreements: 0, 1, several
     * 2. final page size: less than standard(currently standard size: 10), equals standard
     * 3. final page number: less than requested, equals requested
     * 4. Invalid input data:
     * */

    // Test 1.1 get page consisted from several agreements, page size equals standard, page number equals requested
    // Expected: return page with several agreements, page size equals standard
    @Test
    public void getPageCreditAgreementsSeveralAgreementsPageSizeEqualsStandardPageNumberEqualsRequested() {
        final int totalAgreements = 15;
        final int standardPageSize = 10;
        final int pageNumber = 1;

        int expectedPageSize = standardPageSize;
        int expectedTotal = totalAgreements;
        int expectedPage = pageNumber;

        List<CreditAgreementDto> storedAgreementDtos = new ArrayList<>();

        for (int i = 0; i < totalAgreements; i++) {
            var client = Client.builder()
                    .firstName("Ivan")
                    .lastName("Ivanov")
                    .passport(getNextPassport())
                    .phone(phone1)
                    .maritalStatus(MaritalStatus.MARRIED)
                    .address("Address")
                    .organizationName("Organization")
                    .position("Position")
                    .employmentPeriod(Duration.ofDays(200))
                    .build();

            client = clientRepository.save(client);

            var clientApplication = CreditApplication.builder()
                    .client(client)
                    .requestedAmount(BigDecimal.valueOf(10_000))
                    .status(CreditApplicationStatus.REJECTED)
                    .approvedAmount(amountApproved(BigDecimal.valueOf(10_000)))
                    .approvedTerm(30)
                    .createdAt(LocalDateTime.now())
                    .creditAgreement(null)
                    .build();

            clientApplication = creditApplicationRepository.save(clientApplication);

            var clientApplicationAgreement = CreditAgreement.builder()
                    .application(clientApplication)
                    .signedAt(LocalDateTime.now())
                    .signingStatus(SigningStatus.SIGNED)
                    .build();

            var saved = creditAgreementRepository.save(clientApplicationAgreement);
            var savedDto = creditApplicationMapper.fromCreditAgreement(saved);
            storedAgreementDtos.add(savedDto);
        }

        var actual = apmService.getPageCreditAgreements(pageNumber);

        assertEquals(expectedPage, actual.getPage());
        assertEquals(expectedPageSize, actual.getPageSize());
        assertEquals(expectedTotal, actual.getTotal());
        assertEquals(expectedPageSize, actual.getContent().size());

        assertEquals(
                storedAgreementDtos.subList(0, expectedPageSize).stream().map(CreditAgreementDto::getId).toList(),
                actual.getContent().stream().map(CreditAgreementDto::getId).toList()
        );
    }

    // Test 1.2 get page consisted from several agreements, page size less than standard, page number equals requested
    // Expected: return page with several agreements, page size less than standard
    @Test
    public void getPageCreditAgreementsSeveralAgreementsPageSizeLessThanStandardPageNumberEqualsRequested() {
        final int totalAgreements = 15;
        final int standardPageSize = 10;
        final int pageNumber = 2;

        int expectedPageSize = 5;
        int expectedTotal = totalAgreements;
        int expectedPage = pageNumber;

        List<CreditAgreementDto> storedAgreementDtos = new ArrayList<>();

        for (int i = 0; i < totalAgreements; i++) {
            var client = Client.builder()
                    .firstName("Ivan")
                    .lastName("Ivanov")
                    .passport(getNextPassport())
                    .phone(phone1)
                    .maritalStatus(MaritalStatus.MARRIED)
                    .address("Address")
                    .organizationName("Organization")
                    .position("Position")
                    .employmentPeriod(Duration.ofDays(200))
                    .build();

            client = clientRepository.save(client);

            var clientApplication = CreditApplication.builder()
                    .client(client)
                    .requestedAmount(BigDecimal.valueOf(10_000))
                    .status(CreditApplicationStatus.REJECTED)
                    .approvedAmount(amountApproved(BigDecimal.valueOf(10_000)))
                    .approvedTerm(30)
                    .createdAt(LocalDateTime.now())
                    .creditAgreement(null)
                    .build();

            clientApplication = creditApplicationRepository.save(clientApplication);

            var clientApplicationAgreement = CreditAgreement.builder()
                    .application(clientApplication)
                    .signedAt(LocalDateTime.now())
                    .signingStatus(SigningStatus.SIGNED)
                    .build();

            var saved = creditAgreementRepository.save(clientApplicationAgreement);
            var savedDto = creditApplicationMapper.fromCreditAgreement(saved);
            storedAgreementDtos.add(savedDto);
        }

        var actual = apmService.getPageCreditAgreements(pageNumber);

        assertEquals(expectedPage, actual.getPage());
        assertEquals(expectedPageSize, actual.getPageSize());
        assertEquals(expectedTotal, actual.getTotal());
        assertEquals(expectedPageSize, actual.getContent().size());

        assertEquals(
                storedAgreementDtos.subList(standardPageSize, totalAgreements).stream().map(CreditAgreementDto::getId).toList(),
                actual.getContent().stream().map(CreditAgreementDto::getId).toList()
        );
    }

    // Test 1.3 get page consisted from one agreement, page size less than standard, page number less than requested
    // Expected: return page with one agreement, page size less than standard
    @Test
    public void getPageCreditAgreementsOneAgreementPageSizeLessThanStandardPageNumberLessThanRequested() {
        final int totalAgreements = 1;
        final int standardPageSize = 10;
        final int pageNumber = 2;

        int expectedPageSize = 1;
        int expectedTotal = totalAgreements;
        int expectedPage = 1;

        List<CreditAgreementDto> storedAgreementDtos = new ArrayList<>();

        for (int i = 0; i < totalAgreements; i++) {
            var client = Client.builder()
                    .firstName("Ivan")
                    .lastName("Ivanov")
                    .passport(getNextPassport())
                    .phone(phone1)
                    .maritalStatus(MaritalStatus.MARRIED)
                    .address("Address")
                    .organizationName("Organization")
                    .position("Position")
                    .employmentPeriod(Duration.ofDays(200))
                    .build();

            client = clientRepository.save(client);

            var clientApplication = CreditApplication.builder()
                    .client(client)
                    .requestedAmount(BigDecimal.valueOf(10_000))
                    .status(CreditApplicationStatus.REJECTED)
                    .approvedAmount(amountApproved(BigDecimal.valueOf(10_000)))
                    .approvedTerm(30)
                    .createdAt(LocalDateTime.now())
                    .creditAgreement(null)
                    .build();

            clientApplication = creditApplicationRepository.save(clientApplication);

            var clientApplicationAgreement = CreditAgreement.builder()
                    .application(clientApplication)
                    .signedAt(LocalDateTime.now())
                    .signingStatus(SigningStatus.SIGNED)
                    .build();

            var saved = creditAgreementRepository.save(clientApplicationAgreement);
            var savedDto = creditApplicationMapper.fromCreditAgreement(saved);
            storedAgreementDtos.add(savedDto);
        }

        var actual = apmService.getPageCreditAgreements(pageNumber);

        assertEquals(expectedPage, actual.getPage());
        assertEquals(expectedPageSize, actual.getPageSize());
        assertEquals(expectedTotal, actual.getTotal());
        assertEquals(expectedPageSize, actual.getContent().size());

        assertEquals(
                storedAgreementDtos.stream().map(CreditAgreementDto::getId).toList(),
                actual.getContent().stream().map(CreditAgreementDto::getId).toList()
        );
    }

    // Test 1.4 get page consisted from zero agreements, page size less than standard, page number less than requested
    // Expected: return page with zero agreements, page size less than standard
    @Test
    public void getPageCreditAgreementsZeroAgreementsPageSizeLessThanStandardPageNumberLessThanRequested() {
        final int totalAgreements = 0;
        final int standardPageSize = 10;
        final int pageNumber = 5;

        int expectedPageSize = 0;
        int expectedTotal = totalAgreements;
        int expectedPage = 1;

        List<CreditAgreementDto> storedAgreementDtos = new ArrayList<>();

        var actual = apmService.getPageCreditAgreements(pageNumber);

        assertEquals(expectedPage, actual.getPage());
        assertEquals(expectedPageSize, actual.getPageSize());
        assertEquals(expectedTotal, actual.getTotal());
        assertEquals(expectedPageSize, actual.getContent().size());

        assertEquals(
                storedAgreementDtos.stream().map(CreditAgreementDto::getId).toList(),
                actual.getContent().stream().map(CreditAgreementDto::getId).toList()
        );
    }

    // Test 2.1 invalid page number(negative)
    // Expected: throw ConstraintViolationException
    @Test
    public void getPageCreditAgreementsByNegativePageNumber() {
        final int pageNumber = -1;
        assertThrows(ConstraintViolationException.class, () -> apmService.getPageCreditAgreements(pageNumber));
    }

    // Test 2.2 invalid page number(zero)
    // Expected: throw ConstraintViolationException
    @Test
    public void getPageCreditAgreementsByZeroPageNumber() {
        final int pageNumber = 0;
        assertThrows(ConstraintViolationException.class, () -> apmService.getPageCreditAgreements(pageNumber));
    }
}
