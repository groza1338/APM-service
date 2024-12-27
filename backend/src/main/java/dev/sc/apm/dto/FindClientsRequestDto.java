package dev.sc.apm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class FindClientsRequestDto {
    private String firstName;
    private String lastName;
    private String middleName;
    private String phone;
    private String passport;

    public boolean isEmpty() {
        return firstName == null &&
                lastName == null &&
                middleName == null &&
                phone == null &&
                passport == null;
    }
}
