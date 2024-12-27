package dev.sc.apm.service;

import dev.sc.apm.config.TestContainerConfig;
import dev.sc.apm.dto.ClientDto;
import dev.sc.apm.dto.FindClientsRequestDto;
import dev.sc.apm.dto.PageResponseDto;
import dev.sc.apm.entity.Client;
import dev.sc.apm.entity.CreditApplication;
import dev.sc.apm.entity.CreditApplicationStatus;
import dev.sc.apm.entity.MaritalStatus;
import dev.sc.apm.exception.GroupValidationException;
import dev.sc.apm.exception.ValidationException;
import dev.sc.apm.exception.ExceptionName;
import dev.sc.apm.mapper.ClientMapper;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ContextConfiguration(initializers = TestContainerConfig.Initializer.class)
public class ClientServiceTest {
    @SpyBean
    private ClientMapper clientMapper;
    @SpyBean
    private ClientRepository clientRepository;
    @SpyBean
    private CreditApplicationRepository creditApplicationRepository;
    @SpyBean
    private CreditAgreementRepository creditAgreementRepository;

    @Autowired
    private ClientService clientService;

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
     * Test ClientService.findClients(...)
     * Aspect of testing:
     * 1. Count stored clients: 0, 1, several
     * 2. final page size: less than standard(currently standard size: 10), equals standard
     * 3. final page number: less than requested, equals requested
     * 4. filter by: first name, last name, middle name, phone, passport, together, partially
     * 5. invalid input data:
     *      - page number: 0, negative
     *      - invalid first name
     *      - invalid last name
     *      - invalid middle name
     *      - invalid phone
     *      - invalid passport
     * */

    // Test 1.1 get page consisted from several clients, page size equals to standard, page number equals to requested
    @Test
    public void getClientPageWhoseSizeIsEqualsToStandardAndReceivedPageNumberIsEqualsRequested() {

        final int requestedPage = 1;
        final int totalClients = 15;
        final int standardPageSize = 10;

        final int expectedReceivedPage = requestedPage;
        final int expectedReceivedPageSize = standardPageSize;
        final int expectedTotalClients = totalClients;

        List<ClientDto> storedClientDtos = generateClients(totalClients);

        PageResponseDto<ClientDto> actual = clientService.findClients(requestedPage, new FindClientsRequestDto());

        assertEquals(expectedReceivedPage, actual.getPage());
        assertEquals(expectedReceivedPageSize, actual.getPageSize());
        assertEquals(expectedTotalClients, actual.getTotal());
        assertEquals(expectedReceivedPageSize, actual.getContent().size());

        assertEquals(
                storedClientDtos.subList(0, expectedReceivedPageSize),
                actual.getContent()
        );
    }

    private List<ClientDto> generateClients(int countUser) {

        LocalDateTime createAt = LocalDateTime.now();

        List<ClientDto> clientDtos = new ArrayList<>();

        for (int i = 0; i < countUser; i++) {
            var c = Client.builder()
                    .firstName("Ivan")
                    .lastName("Ivanov")
                    .middleName("Ivanovich")
                    .passport(getNextPassport())
                    .phone(phone2)
                    .maritalStatus(MaritalStatus.SEPARATED)
                    .address("Moscow")
                    .organizationName("Sberbank")
                    .position("Manager")
                    .employmentPeriod(Duration.ofDays(400))
                    .build();

            c = clientRepository.save(c);

            var clientApp = CreditApplication.builder()
                    .client(c)
                    .requestedAmount(BigDecimal.valueOf(100_000, 1))
                    .createdAt(createAt)
                    .status(CreditApplicationStatus.PENDING)
                    .build();

            clientApp = creditApplicationRepository.save(clientApp);

            clientDtos.add(clientMapper.fromClient(c));
        }

        return clientDtos;
    }

    // Test 1.2 get page consisted from several clients, page size less than standard, page number equals requested
    @Test
    public void getClientPageWhoseSizeIsLessThanStandardAndReceivedPageNumberIsEqualsRequested() {

        final int requestedPage = 2;
        final int totalClients = 15;
        final int standardPageSize = 10;

        final int expectedReceivedPage = requestedPage;
        final int expectedReceivedPageSize = 5;
        final int expectedTotalClients = totalClients;

        List<ClientDto> storedClientDtos = generateClients(totalClients);

        PageResponseDto<ClientDto> actual = clientService.findClients(requestedPage, new FindClientsRequestDto());

        assertEquals(expectedReceivedPage, actual.getPage());
        assertEquals(expectedReceivedPageSize, actual.getPageSize());
        assertEquals(expectedTotalClients, actual.getTotal());
        assertEquals(expectedReceivedPageSize, actual.getContent().size());

        assertEquals(
                storedClientDtos.subList(standardPageSize, totalClients),
                actual.getContent()
        );
    }

    // Test 1.3 get page consisted from several clients, page size less than standard, page number less than requested
    @Test
    public void getClientPageWhoseSizeIsLessThanStandardAndReceivedPageNumberIsLessThanRequested() {

        final int requestedPage = 2;
        final int totalClients = 9;
        final int standardPageSize = 10;

        final int expectedReceivedPage = 1;
        final int expectedReceivedPageSize = totalClients;
        final int expectedTotalClients = totalClients;

        List<ClientDto> storedClientDtos = generateClients(totalClients);

        PageResponseDto<ClientDto> actual = clientService.findClients(requestedPage, new FindClientsRequestDto());

        assertEquals(expectedReceivedPage, actual.getPage());
        assertEquals(expectedReceivedPageSize, actual.getPageSize());
        assertEquals(expectedTotalClients, actual.getTotal());
        assertEquals(expectedReceivedPageSize, actual.getContent().size());

        assertEquals(
                storedClientDtos.subList(0, expectedReceivedPageSize),
                actual.getContent()
        );
    }

    // Test 1.4 get page consisted from 1 clients, page size less to standard, page number less than requested
    @Test
    public void getClientPageFromOneWhoseSizeIsLessThanStandardAndReceivedPageNumberIsLessThanRequested() {

        final int requestedPage = 2;
        final int totalClients = 1;
        final int standardPageSize = 10;

        final int expectedReceivedPage = 1;
        final int expectedReceivedPageSize = totalClients;
        final int expectedTotalClients = totalClients;

        List<ClientDto> storedClientDtos = generateClients(totalClients);

        PageResponseDto<ClientDto> actual = clientService.findClients(requestedPage, new FindClientsRequestDto());

        assertEquals(expectedReceivedPage, actual.getPage());
        assertEquals(expectedReceivedPageSize, actual.getPageSize());
        assertEquals(expectedTotalClients, actual.getTotal());
        assertEquals(expectedReceivedPageSize, actual.getContent().size());

        assertEquals(
                storedClientDtos.subList(0, expectedReceivedPageSize),
                actual.getContent()
        );
    }

    // Test 1.5 get page consisted from 0 clients, page size less to standard, page number less than requested
    @Test
    public void getClientPageFromZeroWhoseSizeIsLessThanStandardAndReceivedPageNumberIsLessThanRequested() {

        final int requestedPage = 2;
        final int totalClients = 0;
        final int standardPageSize = 10;

        final int expectedReceivedPage = 1;
        final int expectedReceivedPageSize = totalClients;
        final int expectedTotalClients = totalClients;

        List<ClientDto> storedClientDtos = generateClients(totalClients);

        PageResponseDto<ClientDto> actual = clientService.findClients(requestedPage, new FindClientsRequestDto());

        assertEquals(expectedReceivedPage, actual.getPage());
        assertEquals(expectedReceivedPageSize, actual.getPageSize());
        assertEquals(expectedTotalClients, actual.getTotal());
        assertEquals(expectedReceivedPageSize, actual.getContent().size());

        assertEquals(
                storedClientDtos.subList(0, expectedReceivedPageSize),
                actual.getContent()
        );
    }

    // Test 2.1 find by first name
    @Test
    public void findClientsByFirstName() {

        final int requestedPage = 1;
        final int totalClients = 15;
        final int standardPageSize = 10;

        final int expectedReceivedPage = 1;
        final int expectedReceivedPageSize = 2;
        final int expectedTotalClients = 2;

        String searchedParam = "Danil";

        List<ClientDto> storedClientDtos = generateClients(totalClients);

        FindClientsRequestDto findClientRequest = FindClientsRequestDto.builder()
                .firstName(searchedParam)
                .build();

        var c1 = Client.builder()
                .firstName(searchedParam)
                .lastName("Kozlov")
                .passport(getNextPassport())
                .phone(phone2)
                .maritalStatus(MaritalStatus.SEPARATED)
                .address("Moscow")
                .organizationName("AlfaBank")
                .position("Consultant")
                .employmentPeriod(Duration.ofDays(400))
                .build();

        c1 = clientRepository.save(c1);

        storedClientDtos.add(clientMapper.fromClient(c1));

        var c2 = Client.builder()
                .firstName(searchedParam)
                .lastName("Petrov")
                .passport(getNextPassport())
                .phone(phone2)
                .maritalStatus(MaritalStatus.COHABITING)
                .address("Moscow")
                .organizationName("VTB")
                .position("Manager")
                .employmentPeriod(Duration.ofDays(400))
                .build();

        c2 = clientRepository.save(c2);

        storedClientDtos.add(clientMapper.fromClient(c2));

        PageResponseDto<ClientDto> actual = clientService.findClients(requestedPage, findClientRequest);

        assertEquals(expectedReceivedPage, actual.getPage());
        assertEquals(expectedReceivedPageSize, actual.getPageSize());
        assertEquals(expectedTotalClients, actual.getTotal());
        assertEquals(expectedReceivedPageSize, actual.getContent().size());

        assertEquals(
                storedClientDtos.stream().filter(c -> c.getFirstName().equals(searchedParam)).toList(),
                actual.getContent()
        );
    }

    // Test 2.2 find by start with first name
    // Expected: empty
    @Test
    public void findClientsByStartWithFirstName() {

        final int requestedPage = 1;
        final int totalClients = 15;
        final int standardPageSize = 10;

        final int expectedReceivedPage = 1;
        final int expectedReceivedPageSize = 0;
        final int expectedTotalClients = 0;

        String searchedParam = "Da";

        List<ClientDto> storedClientDtos = generateClients(totalClients);

        FindClientsRequestDto findClientRequest = FindClientsRequestDto.builder()
                .firstName(searchedParam)
                .build();

        var c1 = Client.builder()
                .firstName("Danila")
                .lastName("Kozlov")
                .passport(getNextPassport())
                .phone(phone2)
                .maritalStatus(MaritalStatus.SEPARATED)
                .address("Moscow")
                .organizationName("AlfaBank")
                .position("Consultant")
                .employmentPeriod(Duration.ofDays(400))
                .build();

        c1 = clientRepository.save(c1);

        storedClientDtos.add(clientMapper.fromClient(c1));

        var c2 = Client.builder()
                .firstName("Danila")
                .lastName("Petrov")
                .passport(getNextPassport())
                .phone(phone2)
                .maritalStatus(MaritalStatus.COHABITING)
                .address("Moscow")
                .organizationName("VTB")
                .position("Manager")
                .employmentPeriod(Duration.ofDays(400))
                .build();

        c2 = clientRepository.save(c2);

        storedClientDtos.add(clientMapper.fromClient(c2));

        PageResponseDto<ClientDto> actual = clientService.findClients(requestedPage, findClientRequest);

        assertEquals(expectedReceivedPage, actual.getPage());
        assertEquals(expectedReceivedPageSize, actual.getPageSize());
        assertEquals(expectedTotalClients, actual.getTotal());
        assertEquals(expectedReceivedPageSize, actual.getContent().size());

        assertEquals(
                storedClientDtos.stream().filter(c -> c.getFirstName().equals(searchedParam)).toList(),
                actual.getContent()
        );
    }

    // Test 2.3 find by last name
    @Test
    public void findClientsByLastName() {

        final int requestedPage = 1;
        final int totalClients = 15;
        final int standardPageSize = 10;

        final int expectedReceivedPage = 1;
        final int expectedReceivedPageSize = 2;
        final int expectedTotalClients = 2;

        String searchedParam = "Kozlov";

        List<ClientDto> storedClientDtos = generateClients(totalClients);

        FindClientsRequestDto findClientRequest = FindClientsRequestDto.builder()
                .lastName(searchedParam)
                .build();

        var c1 = Client.builder()
                .firstName("Danil")
                .lastName(searchedParam)
                .passport(getNextPassport())
                .phone(phone2)
                .maritalStatus(MaritalStatus.SEPARATED)
                .address("Moscow")
                .organizationName("AlfaBank")
                .position("Consultant")
                .employmentPeriod(Duration.ofDays(400))
                .build();

        c1 = clientRepository.save(c1);

        storedClientDtos.add(clientMapper.fromClient(c1));

        var c2 = Client.builder()
                .firstName("Ivan")
                .lastName(searchedParam)
                .passport(getNextPassport())
                .phone(phone2)
                .maritalStatus(MaritalStatus.COHABITING)
                .address("Moscow")
                .organizationName("VTB")
                .position("Manager")
                .employmentPeriod(Duration.ofDays(400))
                .build();

        c2 = clientRepository.save(c2);

        storedClientDtos.add(clientMapper.fromClient(c2));

        PageResponseDto<ClientDto> actual = clientService.findClients(requestedPage, findClientRequest);

        assertEquals(expectedReceivedPage, actual.getPage());
        assertEquals(expectedReceivedPageSize, actual.getPageSize());
        assertEquals(expectedTotalClients, actual.getTotal());
        assertEquals(expectedReceivedPageSize, actual.getContent().size());

        assertEquals(
                storedClientDtos.stream().filter(c -> c.getLastName().equals(searchedParam)).toList(),
                actual.getContent()
        );
    }

    // Test 2.4 find by middle name
    @Test
    public void findClientsByMiddleName() {

        final int requestedPage = 1;
        final int totalClients = 15;
        final int standardPageSize = 10;

        final int expectedReceivedPage = 1;
        final int expectedReceivedPageSize = 2;
        final int expectedTotalClients = 2;

        String searchedParam = "Anreevich";

        List<ClientDto> storedClientDtos = generateClients(totalClients);

        FindClientsRequestDto findClientRequest = FindClientsRequestDto.builder()
                .middleName(searchedParam)
                .build();

        var c1 = Client.builder()
                .firstName("Danil")
                .lastName("Kozlov")
                .middleName(searchedParam)
                .passport(getNextPassport())
                .phone(phone2)
                .maritalStatus(MaritalStatus.SEPARATED)
                .address("Moscow")
                .organizationName("AlfaBank")
                .position("Consultant")
                .employmentPeriod(Duration.ofDays(400))
                .build();

        c1 = clientRepository.save(c1);

        storedClientDtos.add(clientMapper.fromClient(c1));

        var c2 = Client.builder()
                .firstName("Ivan")
                .lastName("Petrov")
                .middleName(searchedParam)
                .passport(getNextPassport())
                .phone(phone2)
                .maritalStatus(MaritalStatus.COHABITING)
                .address("Moscow")
                .organizationName("VTB")
                .position("Manager")
                .employmentPeriod(Duration.ofDays(400))
                .build();

        c2 = clientRepository.save(c2);

        storedClientDtos.add(clientMapper.fromClient(c2));

        PageResponseDto<ClientDto> actual = clientService.findClients(requestedPage, findClientRequest);

        assertEquals(expectedReceivedPage, actual.getPage());
        assertEquals(expectedReceivedPageSize, actual.getPageSize());
        assertEquals(expectedTotalClients, actual.getTotal());
        assertEquals(expectedReceivedPageSize, actual.getContent().size());

        assertEquals(
                storedClientDtos.stream().filter(c -> c.getMiddleName().equals(searchedParam)).toList(),
                actual.getContent()
        );
    }

    // Test 2.5 find by phone
    @Test
    public void findClientsByPhone() {

        final int requestedPage = 2;
        final int totalClients = 15;
        final int standardPageSize = 10;

        final int expectedReceivedPage = 2;
        final int expectedReceivedPageSize = 7;

        String searchedParam = phone2;

        List<ClientDto> storedClientDtos = generateClients(totalClients);

        FindClientsRequestDto findClientRequest = FindClientsRequestDto.builder()
                .phone(searchedParam)
                .build();

        var c1 = Client.builder()
                .firstName("Danil")
                .lastName("Kozlov")
                .passport(getNextPassport())
                .phone(phone2)
                .maritalStatus(MaritalStatus.SEPARATED)
                .address("Moscow")
                .organizationName("AlfaBank")
                .position("Consultant")
                .employmentPeriod(Duration.ofDays(400))
                .build();

        c1 = clientRepository.save(c1);

        storedClientDtos.add(clientMapper.fromClient(c1));

        var c2 = Client.builder()
                .firstName("Ivan")
                .lastName("Petrov")
                .passport(getNextPassport())
                .phone(phone2)
                .maritalStatus(MaritalStatus.COHABITING)
                .address("Moscow")
                .organizationName("VTB")
                .position("Manager")
                .employmentPeriod(Duration.ofDays(400))
                .build();

        c2 = clientRepository.save(c2);

        storedClientDtos.add(clientMapper.fromClient(c2));

        final int expectedTotalClients = storedClientDtos.size();

        PageResponseDto<ClientDto> actual = clientService.findClients(requestedPage, findClientRequest);

        assertEquals(expectedReceivedPage, actual.getPage());
        assertEquals(expectedReceivedPageSize, actual.getPageSize());
        assertEquals(expectedTotalClients, actual.getTotal());
        assertEquals(expectedReceivedPageSize, actual.getContent().size());

        assertEquals(
                storedClientDtos.subList(standardPageSize, storedClientDtos.size()).stream()
                        .filter(c -> c.getPhone().equals(searchedParam))
                        .sorted(Comparator.comparingLong(ClientDto::getId))
                        .toList(),
                actual.getContent().stream()
                        .sorted(Comparator.comparingLong(ClientDto::getId))
                        .toList()
        );
    }

    // Test 2.6 find by passport
    @Test
    public void findByPassport() {

        final int requestedPage = 1;
        final int totalClients = 15;
        final int standardPageSize = 10;

        final int expectedReceivedPage = 1;
        final int expectedReceivedPageSize = 1;
        final int expectedTotalClients = 1;

        List<ClientDto> storedClientDtos = generateClients(totalClients);
        String searchedParam = storedClientDtos.get(totalClients / 2).getPassport();

        FindClientsRequestDto findClientRequest = FindClientsRequestDto.builder()
                .passport(searchedParam)
                .build();

        PageResponseDto<ClientDto> actual = clientService.findClients(requestedPage, findClientRequest);

        assertEquals(expectedReceivedPage, actual.getPage());
        assertEquals(expectedReceivedPageSize, actual.getPageSize());
        assertEquals(expectedTotalClients, actual.getTotal());
        assertEquals(expectedReceivedPageSize, actual.getContent().size());

        assertEquals(
                storedClientDtos.stream().filter(c -> c.getPassport().equals(searchedParam)).toList(),
                actual.getContent()
        );
    }

    // Test 2.7 find by first name, last name, middle name, phone, passport
    @Test
    public void findByAllParams() {

        final int requestedPage = 1;
        final int totalClients = 15;
        final int standardPageSize = 10;

        final int expectedReceivedPage = 1;
        final int expectedReceivedPageSize = 1;
        final int expectedTotalClients = 1;

        List<ClientDto> storedClientDtos = generateClients(totalClients);
        ClientDto searchedClient = storedClientDtos.get(totalClients / 2);

        FindClientsRequestDto findClientRequest = FindClientsRequestDto.builder()
                .firstName(searchedClient.getFirstName())
                .lastName(searchedClient.getLastName())
                .middleName(searchedClient.getMiddleName())
                .phone(searchedClient.getPhone())
                .passport(searchedClient.getPassport())
                .build();

        PageResponseDto<ClientDto> actual = clientService.findClients(requestedPage, findClientRequest);

        assertEquals(expectedReceivedPage, actual.getPage());
        assertEquals(expectedReceivedPageSize, actual.getPageSize());
        assertEquals(expectedTotalClients, actual.getTotal());
        assertEquals(expectedReceivedPageSize, actual.getContent().size());

        assertEquals(
                storedClientDtos.stream().filter(c -> c.equals(searchedClient)).toList(),
                actual.getContent()
        );
    }

    // Test 2.8 find by client by several params
    @Test
    public void findBySeveralParams() {
        final int requestedPage = 1;
        final int totalClients = 15;
        final int standardPageSize = 10;

        final int expectedReceivedPage = 1;
        final int expectedReceivedPageSize = 2;
        final int expectedTotalClients = 2;

        String searchedParam1 = "Danil";
        String searchedParam2 = "Popov";

        List<ClientDto> storedClientDtos = generateClients(totalClients);

        FindClientsRequestDto findClientRequest = FindClientsRequestDto.builder()
                .firstName(searchedParam1)
                .lastName(searchedParam2)
                .build();

        var c1 = Client.builder()
                .firstName(searchedParam1)
                .lastName(searchedParam2)
                .passport(getNextPassport())
                .phone(phone2)
                .maritalStatus(MaritalStatus.SEPARATED)
                .address("Moscow")
                .organizationName("AlfaBank")
                .position("Consultant")
                .employmentPeriod(Duration.ofDays(400))
                .build();

        c1 = clientRepository.save(c1);

        storedClientDtos.add(clientMapper.fromClient(c1));

        var c2 = Client.builder()
                .firstName(searchedParam1)
                .lastName(searchedParam2)
                .passport(getNextPassport())
                .phone(phone1)
                .maritalStatus(MaritalStatus.COHABITING)
                .address("Volgograd")
                .organizationName("VTB")
                .position("Manager")
                .employmentPeriod(Duration.ofDays(400))
                .build();

        c2 = clientRepository.save(c2);

        storedClientDtos.add(clientMapper.fromClient(c2));

        PageResponseDto<ClientDto> actual = clientService.findClients(requestedPage, findClientRequest);

        assertEquals(expectedReceivedPage, actual.getPage());
        assertEquals(expectedReceivedPageSize, actual.getPageSize());
        assertEquals(expectedTotalClients, actual.getTotal());
        assertEquals(expectedReceivedPageSize, actual.getContent().size());

        assertEquals(
                storedClientDtos.stream().filter(
                        c -> c.getFirstName().equals(searchedParam1) &&
                                c.getLastName().equals(searchedParam2)
                ).toList(),
                actual.getContent()
        );
    }

    // Test 3.1 invalid page number: 0
    @Test
    public void findClientByZeroPage() {
        assertThrows(
                ConstraintViolationException.class,
                () -> clientService.findClients(0, new FindClientsRequestDto())
        );
    }

    // Test 3.2 invalid page number: negative
    @Test
    public void findClientByNegativePage() {
        assertThrows(
                ConstraintViolationException.class,
                () -> clientService.findClients(-1, new FindClientsRequestDto())
        );
    }

    // Test 3.3 invalid first name, last name, middle name, phone, passport
    @Test
    public void findClientByTogetherInvalidParams() {

        GroupValidationException groupValidationException = null;
        try {
            clientService.findClients(1, FindClientsRequestDto.builder()
                    .firstName("Iv3an")
                    .lastName("Ivan%ov")
                    .middleName("Iva@novich")
                    .phone("899912345683")
                    .passport("01234567890")
                    .build()
            );
            fail();
        } catch (GroupValidationException e) {
            groupValidationException = e;
        } catch (Exception e) {
            fail();
        }

        assertEquals(5, groupValidationException.exceptions().size());

        List<ExceptionName> expected = List.of(
                ExceptionName.INVALID_FIRST_NAME,
                ExceptionName.INVALID_LAST_NAME,
                ExceptionName.INVALID_MIDDLE_NAME,
                ExceptionName.INVALID_PHONE,
                ExceptionName.INVALID_PASSPORT
        );

        List<ExceptionName> actual = groupValidationException.exceptions().stream()
                .map(ValidationException::getExpName)
                .toList();

        assertEquals(expected, actual);
    }
}
