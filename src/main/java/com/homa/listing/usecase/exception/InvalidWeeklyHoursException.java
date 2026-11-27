package com.homa.listing.usecase.exception;

/** Weekly hours that aren't one "DAY HH:MM-HH:MM" entry per working day, such as "SATURDAY 08:00-18:00". */
public final class InvalidWeeklyHoursException extends RuntimeException {

    public InvalidWeeklyHoursException(String message) {
        super(message);
    }
}
