package com.homa.rating.usecase.exception;

/** No ratings are open for the reservation: only a Completed reservation gets ratings (RTG-9). */
public final class RatingSessionNotFoundException extends RatingUsecaseException {

    private static final long serialVersionUID = 1L;

    public RatingSessionNotFoundException(String reservationId) {
        super("RTG-9: no ratings are open for reservation " + reservationId);
    }
}
