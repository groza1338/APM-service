package dev.sc.apm.dto;

import dev.sc.apm.entity.MaritalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ClientDto {
    private long id;
    private String firstName;
    private String lastName;
    private String middleName;
    private String passport;
    private MaritalStatus maritalStatus;
    private String address;
    private String phone;
    private String organizationName;
    private String position;
    private Duration employmentPeriod;
}
