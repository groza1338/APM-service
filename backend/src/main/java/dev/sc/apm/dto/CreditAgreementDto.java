package dev.sc.apm.dto;

import dev.sc.apm.entity.SigningStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class CreditAgreementDto {
    private long id;
    private long applicationId;
    private LocalDateTime signedAt;
    private SigningStatus signingStatus;
}
