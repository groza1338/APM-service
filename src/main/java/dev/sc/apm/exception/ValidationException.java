package dev.sc.apm.exception;

import lombok.Getter;
import lombok.Setter;

public class ValidationException extends BaseException {

    public ValidationException(String reason, ExceptionName title) {
        super(reason);
        this.expName = title;
    }

    public ValidationException(String reason) {
        super(reason);
    }
}
