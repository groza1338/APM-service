package dev.sc.apm.controller;

import dev.sc.apm.exception.CreditApplicationNotFound;
import dev.sc.apm.exception.GroupValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CreditApplicationNotFound.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponseException handleCreditApplicationNotFound(CreditApplicationNotFound e) {
        return buildErrorResponseException(
                HttpStatus.NOT_FOUND,
                "credit_application_not_found",
                e,
                "not_found",
                Map.of(e.getExpName().name(), e.reason())
        );
    }

    @ExceptionHandler(GroupValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseException handleValidationException(GroupValidationException e) {
        return buildErrorResponseException(
                HttpStatus.BAD_REQUEST,
                "group_validation_exception",
                e,
                "invalid_property",
                e.toMap()
        );
    }

    private ErrorResponseException buildErrorResponseException(
            HttpStatus status,
            String reason,
            Throwable cause,
            String bodyName,
            Map<String, String> body
    ) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                status,
                reason != null ? reason : cause.getMessage()
        );

        detail.setType(URI.create("error"));
        detail.setProperty("date", LocalDateTime.now());

        if (body != null) {
            detail.setProperty(bodyName, body);
        }

        return new ErrorResponseException(status, detail, cause);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponseException handleException(Exception e) {
        log.error(e.getMessage(), e);
        return buildErrorResponseException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                null,
                e,
                null,
                null
        );
    }
}
