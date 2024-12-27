package dev.sc.apm.exception;

public class ClientInfoMismatchException extends ValidationException {
    public ClientInfoMismatchException() {
        super(
                "The data provided does not match the information about the client available in the system.",
                ExceptionName.MISMATCHED_CLIENT_INFO
        );
    }
}
