package com.homa.reservation.usecase.exception;

/** The photo store holds no uploaded photo with this reference. */
public final class CompletionPhotoNotFoundException extends ReservationUsecaseException {

    private static final long serialVersionUID = 1L;

    public CompletionPhotoNotFoundException(String photoRef) {
        super("No uploaded photo " + photoRef);
    }
}
