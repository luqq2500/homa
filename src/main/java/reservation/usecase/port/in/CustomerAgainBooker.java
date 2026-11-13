package reservation.usecase.port.in;

import reservation.usecase.dto.BookAgainRequest;
import reservation.usecase.dto.BookAgainResponse;

/** A customer starts a new enquiry from a past enquiry or reservation, at a date and time they pick. */
public interface CustomerAgainBooker {

    BookAgainResponse bookAgain(BookAgainRequest request);
}
