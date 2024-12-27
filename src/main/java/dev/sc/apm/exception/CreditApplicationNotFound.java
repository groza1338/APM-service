package dev.sc.apm.exception;

public class CreditApplicationNotFound extends BaseException {

    public CreditApplicationNotFound(long applicationId) {
        super(
                "Credit application with id " + applicationId + " not found.",
                ExceptionName.CREDIT_APPLICATION_NOT_FOUND);
    }
}
