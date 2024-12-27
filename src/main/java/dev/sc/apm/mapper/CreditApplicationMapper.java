package dev.sc.apm.mapper;

import dev.sc.apm.dto.CreditAgreementDto;
import dev.sc.apm.dto.CreditApplicationDto;
import dev.sc.apm.entity.CreditAgreement;
import dev.sc.apm.entity.CreditApplication;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.FIELD,
        unmappedSourcePolicy = ReportingPolicy.IGNORE
)
public interface CreditApplicationMapper {
    @Mapping(source = "client.id", target = "applicantId")
    @Mapping(source = "creditAgreement.id", target = "creditAgreementId")
    CreditApplicationDto fromCreditApplication(CreditApplication creditApplication);

    @Mapping(source = "application.id", target = "applicationId")
    CreditAgreementDto fromCreditAgreement(CreditAgreement creditApplication);
}
