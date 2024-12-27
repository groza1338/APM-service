package dev.sc.apm.exception;

public class PendingStatusDuringSigningException extends ValidationException {
    public PendingStatusDuringSigningException(long applicationId) {
        super(
                "Unable to sign application with id " + applicationId + " because it is in PENDING status.",
                ExceptionName.PENDING_STATUS_DURING_SIGNING);
    }
}
