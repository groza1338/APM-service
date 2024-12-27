package dev.sc.apm.mapper;

import dev.sc.apm.dto.ClientDto;
import dev.sc.apm.dto.CreditApplicantDto;
import dev.sc.apm.entity.Client;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.FIELD,
        unmappedSourcePolicy = ReportingPolicy.IGNORE
)
public interface ClientMapper {

    @Mapping(target = "id", ignore = true)
    Client fromCreditApplicantDto(CreditApplicantDto creditApplicantDto);
    CreditApplicantDto toCreditApplicantDto(Client client);

    ClientDto fromClient(Client client);
}
