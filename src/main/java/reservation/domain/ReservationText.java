package reservation.domain;

import reservation.domain.exception.ReservationDetailRequiredException;

/** Text on a reservation is required and kept without surrounding spaces. */
final class ReservationText {

    private ReservationText() {
    }

    static String require(String text, String what) {
        if (text == null || text.isBlank()) {
            throw new ReservationDetailRequiredException(what);
        }
        return text.strip();
    }
}
