package dev.sc.apm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class CreditApplicationRequestDto {
    @Valid
    @NotNull(message = "Applicant information is required")
    private CreditApplicantDto applicant;

    @NotNull(message = "Loan amount is required")
    @DecimalMin(value = "1", message = "Loan amount must be greater than 0")
    private BigDecimal amount;
}
