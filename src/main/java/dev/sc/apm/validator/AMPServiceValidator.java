package dev.sc.apm.validator;

import dev.sc.apm.dto.FindClientsRequestDto;
import dev.sc.apm.exception.GroupValidationException;
import dev.sc.apm.exception.ValidationException;
import dev.sc.apm.exception.ExceptionName;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class AMPServiceValidator {

    private final int MAX_NAME_LENGTH;
    private final String NAME_PATTERN;
    private final int PHONE_LENGTH;
    private final String PHONE_PATTERN;
    private final int PASSPORT_LENGTH;
    private final String PASSPORT_PATTERN;

    public AMPServiceValidator(
            int maxNameLength,
            String namePattern,
            int phoneLength,
            String phonePattern,
            int passportLength,
            String passportPattern
    ) {
        MAX_NAME_LENGTH = maxNameLength;
        NAME_PATTERN = namePattern;
        PHONE_LENGTH = phoneLength;
        PHONE_PATTERN = phonePattern;
        PASSPORT_LENGTH = passportLength;
        PASSPORT_PATTERN = passportPattern;
    }

    public Optional<GroupValidationException> validateFindClientRequestDto(FindClientsRequestDto dto) {
        List<ValidationException> exps = new ArrayList<>();

        if (dto.getFirstName() != null) {
            Optional<ValidationException> nameValidation = validateName(dto.getFirstName());
            if (nameValidation.isPresent()) {
                nameValidation.get().setExpName(ExceptionName.INVALID_FIRST_NAME);
                exps.add(nameValidation.get());
            }
        }

        if (dto.getLastName() != null) {
            Optional<ValidationException> nameValidation = validateName(dto.getLastName());
            if (nameValidation.isPresent()) {
                nameValidation.get().setExpName(ExceptionName.INVALID_LAST_NAME);
                exps.add(nameValidation.get());
            }
        }

        if (dto.getMiddleName() != null) {
            Optional<ValidationException> nameValidation = validateName(dto.getMiddleName());
            if (nameValidation.isPresent()) {
                nameValidation.get().setExpName(ExceptionName.INVALID_MIDDLE_NAME);
                exps.add(nameValidation.get());
            }
        }

        if (dto.getPhone() != null) {
            Optional<ValidationException> phoneValidation = validatePhone(dto.getPhone());
            phoneValidation.ifPresent(exps::add);
        }

        if (dto.getPassport() != null) {
            Optional<ValidationException> passportValidation = validatePassport(dto.getPassport());
            passportValidation.ifPresent(exps::add);
        }

        if (exps.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new GroupValidationException(exps));
    }

    public Optional<ValidationException> validateName(String name) {
        String nameRule = "The name must not contain white space or/and numbers. " +
                "Its length must be greater than 0 and less than " + MAX_NAME_LENGTH + "characters.";

        if (name.isBlank() && name.length() > MAX_NAME_LENGTH) {
            return Optional.of(new ValidationException(name));
        }

        boolean allCharactersAreValid = name.matches(NAME_PATTERN);
        if (!allCharactersAreValid) {
            return Optional.of(new ValidationException(nameRule));
        }

        return Optional.empty();
    }

    public Optional<ValidationException> validatePhone(String phone) {
        String phoneRule = "The phone must contain only numbers, spaces, hyphens, plus signs, and parentheses. " +
                "Its length must be equals " + PHONE_LENGTH + "characters.";

        if (phone.isBlank() && phone.length() > PHONE_LENGTH) {
            return Optional.of(new ValidationException(phoneRule, ExceptionName.INVALID_PHONE));
        }

        boolean allCharactersAreValid = phone.matches(PHONE_PATTERN);

        if (!allCharactersAreValid) {
            return Optional.of(new ValidationException(phoneRule, ExceptionName.INVALID_PHONE));
        }

        return Optional.empty();
    }

    public Optional<ValidationException> validatePassport(String passport) {
        String passportRule = "The passport must contain only numbers. " +
                "Its length must be equals " + PASSPORT_LENGTH + "characters.";

        if (passport.isBlank() && passport.length() > PASSPORT_LENGTH) {
            return Optional.of(new ValidationException(passportRule, ExceptionName.INVALID_PASSPORT));
        }

        boolean allCharactersAreValid = Pattern.matches(PASSPORT_PATTERN, passport);

        if (!allCharactersAreValid) {
            return Optional.of(new ValidationException(passportRule, ExceptionName.INVALID_PASSPORT));
        }

        return Optional.empty();
    }
}
