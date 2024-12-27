package dev.sc.apm.exception;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class GroupValidationException extends RuntimeException {

    private final List<ValidationException> exceptions;

    public Map<String, String> toMap() {
        return exceptions.stream()
                .collect(Collectors.toMap(
                        e -> e.expName.name(),
                        ValidationException::reason
                ));
    }

    public List<ValidationException> exceptions() {
        return exceptions;
    }
}
