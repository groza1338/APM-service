package dev.sc.apm.service;

import dev.sc.apm.dto.ClientDto;
import dev.sc.apm.dto.FindClientsRequestDto;
import dev.sc.apm.dto.PageResponseDto;
import dev.sc.apm.entity.Client;
import dev.sc.apm.exception.GroupValidationException;
import dev.sc.apm.mapper.ClientMapper;
import dev.sc.apm.repository.Page;
import dev.sc.apm.repository.Pageable;
import dev.sc.apm.validator.AMPServiceValidator;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import dev.sc.apm.repository.ClientRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static dev.sc.apm.service.ServiceUtil.getPageResponse;

@Service
@Validated
public class ClientService {

    private final ClientRepository clientRepository;

    private final ClientMapper clientMapper;

    private final AMPServiceValidator validator;

    private final int CLIENT_PAGE_SIZE;

    public ClientService(
            ClientRepository clientRepository,
            ClientMapper clientMapper,
            AMPServiceValidator validator,
            @Qualifier("defaultPageSize") int clientPageSize
    ) {
        this.clientRepository = clientRepository;
        this.clientMapper = clientMapper;
        this.validator = validator;
        CLIENT_PAGE_SIZE = clientPageSize;
    }

    @Transactional(readOnly = true)
    public PageResponseDto<ClientDto> findClients(@Valid @Positive int page,@Valid @NotNull FindClientsRequestDto findClientRequest) {

        Optional<GroupValidationException> validation = validator.validateFindClientRequestDto(findClientRequest);

        if (validation.isPresent()) {
            throw validation.get();
        }

        Page<Client> clients = clientRepository.findAllBy(new Pageable(page, CLIENT_PAGE_SIZE), (builder, root) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (findClientRequest.getFirstName() != null) {
                predicates.add(builder.equal(root.get("firstName"), findClientRequest.getFirstName()));
            }

            if (findClientRequest.getLastName() != null) {
                predicates.add(builder.equal(root.get("lastName"), findClientRequest.getLastName()));
            }

            if (findClientRequest.getMiddleName() != null) {
                predicates.add(builder.equal(root.get("middleName"), findClientRequest.getMiddleName()));
            }

            if (findClientRequest.getPhone() != null) {
                predicates.add(builder.equal(root.get("phone"), findClientRequest.getPhone()));
            }

            if (findClientRequest.getPassport() != null) {
                predicates.add(builder.equal(root.get("passport"), findClientRequest.getPassport()));
            }

            return predicates.toArray(Predicate[]::new);
        });

        return getPageResponse(() -> clients, clientMapper::fromClient);
    }
}
