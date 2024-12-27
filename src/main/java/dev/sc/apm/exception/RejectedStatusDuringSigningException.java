package dev.sc.apm.exception;

public class RejectedStatusDuringSigningException extends ValidationException {
    public RejectedStatusDuringSigningException(long applicationId) {
        super(
                "Unable to sign application with id " + applicationId + " because it is in REJECTED status.",
                ExceptionName.REJECTED_STATUS_DURING_SIGNING);
    }
}
