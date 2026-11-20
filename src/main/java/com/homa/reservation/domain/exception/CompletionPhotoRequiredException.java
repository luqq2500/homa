package com.homa.reservation.domain.exception;

/** JOB-3: marking a job Completed needs at least one after-photo. */
public final class CompletionPhotoRequiredException extends ReservationDomainException {

    private static final long serialVersionUID = 1L;

    public CompletionPhotoRequiredException() {
        super("Marking a job Completed needs at least one after-photo");
    }
}
