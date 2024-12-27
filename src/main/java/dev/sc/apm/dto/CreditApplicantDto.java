package dev.sc.apm.dto;

import dev.sc.apm.entity.MaritalStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;


@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class CreditApplicantDto {
    @NotBlank
    @Size(min = 1,max = 64)
    @Pattern(regexp = "^[A-Z][a-z]*(?:[ '-][A-Za-z]+)*$", message = "Invalid first name format")
    private String firstName;

    @NotBlank
    @Size(min = 1, max = 64)
    @Pattern(regexp = "^[A-Z][a-z]*(?:[ '-][A-Za-z]+)*$", message = "Invalid second name format")
    private String lastName;

    @Size(max = 64)
    @Pattern(regexp = "^[A-Z][a-z]*(?:[ '-][A-Za-z]+)*$", message = "Invalid middle name format")
    private String middleName;

    @NotBlank
    @Size(min = 10, max = 10)
    @Pattern(regexp = "^\\d{10}$", message = "Passport must consist of exactly 10 digits")
    private String passport;


    private MaritalStatus maritalStatus;

    @Size(max = 128)
    private String address;

    @NotBlank
    @Size(min = 11, max = 12)
    @Pattern(regexp = "^\\+?\\d{11}$", message = "Phone number must match the format ^[0-9+\\-() ]+$")
    private String phone;

    @NotBlank
    @Size(min = 1, max = 96)
    private String organizationName;

    @NotBlank
    @Size(min = 1, max = 64)
    private String position;

    @NotNull
    private Duration employmentPeriod;
}
