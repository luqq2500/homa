package reservation.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import reservation.domain.exception.EnquiryAlreadyRequestedException;
import reservation.domain.exception.EnquiryDateInPastException;

/**
 * A customer's chosen details before they request, so they can chat with the provider first. An enquiry
 * has no payment and is not a reservation. It turns into one reservation at most, and closes when its date
 * passes without a request.
 */
public final class Enquiry {

    private final String id;
    private final String customerId;
    private final String providerId;
    private final ReservationDetails details;
    private final Instant createdAt;
    private String reservationId;

    private Enquiry(String customerId, String providerId, ReservationDetails details, Instant createdAt) {
        this.id = new EnquiryId().generate();
        this.customerId = customerId;
        this.providerId = providerId;
        this.details = details;
        this.createdAt = createdAt;
    }

    public static Enquiry create(String customerId, String providerId, ReservationDetails details, Instant now) {
        if (!details.start().isAfter(now)) {
            throw new EnquiryDateInPastException(details.start());
        }
        return new Enquiry(
                ReservationText.require(customerId, "A customer id"),
                ReservationText.require(providerId, "A provider id"),
                details,
                Objects.requireNonNull(now));
    }

    /** A new enquiry with the same customer, provider and details as {@code past}, for a new start. */
    public static Enquiry bookAgainFrom(Enquiry past, Instant newStart, Instant now) {
        return create(past.customerId, past.providerId, past.details.startingAt(newStart), now);
    }

    /** An enquiry that was never requested closes when its booked start passes. */
    public boolean isClosed(Instant now) {
        return reservationId == null && !now.isBefore(details.start());
    }

    /** Records the reservation this enquiry turned into. */
    public void markRequested(String newReservationId) {
        if (reservationId != null) {
            throw new EnquiryAlreadyRequestedException(reservationId);
        }
        reservationId = ReservationText.require(newReservationId, "A reservation id");
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

    public ReservationDetails details() {
        return details;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Optional<String> reservationId() {
        return Optional.ofNullable(reservationId);
    }
}
