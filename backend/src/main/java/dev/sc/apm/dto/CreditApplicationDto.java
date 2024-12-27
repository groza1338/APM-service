package dev.sc.apm.dto;

import dev.sc.apm.entity.CreditApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class CreditApplicationDto {
    private long id;
    private long applicantId;
    private BigDecimal requestedAmount;
    private CreditApplicationStatus status;
    private BigDecimal approvedAmount;
    private Integer approvedTerm;
    private LocalDateTime createdAt;
    private Long creditAgreementId;
}
