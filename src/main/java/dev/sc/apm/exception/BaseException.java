package dev.sc.apm.exception;

import lombok.Getter;
import lombok.Setter;

public class BaseException extends RuntimeException {
    @Getter
    @Setter
    protected ExceptionName expName;
    protected final String reason;

    public BaseException(String reason, ExceptionName title) {
        super(reason);
        this.expName = title;
        this.reason = reason;
    }

    public BaseException(String reason) {
        super(reason);
        this.reason = reason;
    }

    public String reason() {
        return reason;
    }
}
