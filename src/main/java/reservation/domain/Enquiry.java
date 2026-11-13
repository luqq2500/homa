package reservation.domain;

import java.time.Instant;
import reservation.domain.exception.EnquiryClosedException;

/**
 * A customer who wants to talk before requesting picks the jobs, date and time, duration and helpers
 * and taps Chat. An enquiry has its own chat, no payment, and is not a reservation status.
 */
public final class Enquiry {

    private final String id;
    private final String customerId;
    private final String providerId;
    private final BookingDetails details;
    private String reservationId;
    private boolean closed;

    public Enquiry(String id, String customerId, String providerId, BookingDetails details, String reservationId,
                   boolean closed) {
        this.id = id;
        this.customerId = customerId;
        this.providerId = providerId;
        this.details = details;
        this.reservationId = reservationId;
        this.closed = closed;
    }

    public static Enquiry start(String customerId, String providerId, BookingDetails details) {
        return new Enquiry(new EnquiryId().generate(), customerId, providerId, details, null, false);
    }

    /** Book again: a new enquiry with the same provider, jobs, duration and helpers, at a date and time the customer picks. */
    public static Enquiry bookAgain(String customerId, String providerId, BookingDetails past, Instant newStart) {
        return start(customerId, providerId, new BookingDetails(past.jobIds(), newStart, past.hours(), past.helpers()));
    }

    /** The customer reserved from the chat: the enquiry becomes this reservation, and the same chat carries on. */
    public void becomeReservation(String reservationId) {
        if (closed || this.reservationId != null) {
            throw new EnquiryClosedException(id);
        }
        this.reservationId = reservationId;
    }

    /** An enquiry that is never requested closes when its date passes. Returns whether it closed just now. */
    public boolean closeIfDatePassed(Instant now) {
        if (closed || reservationId != null || now.isBefore(details.start())) {
            return false;
        }
        closed = true;
        return true;
    }

    public String id() {
        return id;
    }

    public String customerId() {
        return customerId;
    }

    public String providerId() {
        return providerId;
    }

    public BookingDetails details() {
        return details;
    }

    public String reservationId() {
        return reservationId;
    }

    public boolean isClosed() {
        return closed;
    }
}
