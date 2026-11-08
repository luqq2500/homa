package reservation.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import kernel.Money;
import reservation.domain.exception.CityNotCoveredException;
import reservation.domain.exception.HelpersOutsideProviderRangeException;
import reservation.domain.exception.HoursOutsideProviderRangeException;
import reservation.domain.exception.InvalidStatusTransitionException;
import reservation.domain.exception.JobNotOfferedException;
import reservation.domain.exception.RequestExpiredException;
import reservation.domain.exception.RequestNotYetExpiredException;
import reservation.domain.exception.ReservationNotEditableException;
import reservation.domain.exception.StartBeyondHorizonException;
import reservation.domain.exception.StartNotOnQuarterHourException;
import reservation.domain.exception.StartTooSoonException;

/**
 * A reservation, from the customer's request to its ending. It keeps the offering and the Homa-set
 * values in force when it was requested, and every time-based rule of its lifecycle.
 */
public final class Reservation {

    private static final long QUARTER_HOUR_SECONDS = Duration.ofMinutes(15).toSeconds();

    private final String id;
    private final String customerId;
    private final OfferingSnapshot offering;
    private final PolicySnapshot policy;
    private final Optional<String> enquiryId;
    private final PaymentMethod paymentMethod;
    private final Instant requestedAt;
    private ReservationDetails details;
    private PriceBreakdown price;
    private ReservationStatus status;

    private Reservation(
            String customerId,
            ReservationDetails details,
            OfferingSnapshot offering,
            PolicySnapshot policy,
            Optional<String> enquiryId,
            PaymentMethod paymentMethod,
            Instant requestedAt) {
        this.id = new ReservationId().generate();
        this.customerId = customerId;
        this.offering = offering;
        this.policy = policy;
        this.enquiryId = enquiryId;
        this.paymentMethod = paymentMethod;
        this.requestedAt = requestedAt;
        this.details = details;
        this.price = priceFor(details);
        this.status = ReservationStatus.REQUESTED;
    }

    // ---- Request ----

    /** Submits a request for the provider to accept, with the offering and policy in force {@code now}. */
    public static Reservation request(
            String customerId,
            ReservationDetails details,
            OfferingSnapshot offering,
            PolicySnapshot policy,
            Optional<String> enquiryId,
            PaymentMethod paymentMethod,
            Instant now) {
        requireFits(details, offering, policy, now);
        return new Reservation(
                ReservationText.require(customerId, "A customer id"),
                details,
                offering,
                policy,
                Objects.requireNonNull(enquiryId),
                Objects.requireNonNull(paymentMethod),
                now);
    }

    public PriceBreakdown price() {
        return price;
    }

    /** A request expires after the request expiry time, or at the booked start if that comes sooner. */
    public Instant requestExpiresAt() {
        Instant afterExpiryTime = requestedAt.plus(policy.requestExpiry());
        return afterExpiryTime.isBefore(details.start()) ? afterExpiryTime : details.start();
    }

    /** Changes the request before the provider accepts it. The expiry still runs from the original request. */
    public PriceChange edit(ReservationDetails newDetails, Instant now) {
        if (status != ReservationStatus.REQUESTED) {
            throw new ReservationNotEditableException(status);
        }
        requireNotExpired(now);
        requireFits(newDetails, offering, policy, now);
        Money previous = price.total();
        details = newDetails;
        price = priceFor(newDetails);
        return new PriceChange(previous, price.total());
    }

    public void accept(Instant now) {
        requireStatus(ReservationStatus.REQUESTED, "accepted");
        requireNotExpired(now);
        status = ReservationStatus.RESERVED;
    }

    public void decline(Instant now) {
        requireStatus(ReservationStatus.REQUESTED, "declined");
        requireNotExpired(now);
        status = ReservationStatus.DECLINED;
    }

    /** Ends a request the provider did not answer in time. */
    public void expire(Instant now) {
        requireStatus(ReservationStatus.REQUESTED, "expired");
        if (now.isBefore(requestExpiresAt())) {
            throw new RequestNotYetExpiredException(requestExpiresAt());
        }
        status = ReservationStatus.EXPIRED;
    }

    // ---- What the reservation holds ----

    public String id() {
        return id;
    }

    public String customerId() {
        return customerId;
    }

    public String providerId() {
        return offering.providerId();
    }

    public ReservationDetails details() {
        return details;
    }

    public ReservationStatus status() {
        return status;
    }

    public OfferingSnapshot offering() {
        return offering;
    }

    public PolicySnapshot policy() {
        return policy;
    }

    public Optional<String> enquiryId() {
        return enquiryId;
    }

    public PaymentMethod paymentMethod() {
        return paymentMethod;
    }

    public Instant requestedAt() {
        return requestedAt;
    }

    // ---- Rules ----

    private PriceBreakdown priceFor(ReservationDetails chosen) {
        return PriceBreakdown.of(offering, chosen.helpers(), chosen.hours(), policy.serviceFee());
    }

    private void requireStatus(ReservationStatus expected, String action) {
        if (status != expected) {
            throw new InvalidStatusTransitionException(status, action);
        }
    }

    private void requireNotExpired(Instant now) {
        if (!now.isBefore(requestExpiresAt())) {
            throw new RequestExpiredException(requestExpiresAt());
        }
    }

    private static void requireFits(ReservationDetails details, OfferingSnapshot offering, PolicySnapshot policy, Instant now) {
        requireBookableStart(details.start(), policy, now);
        requireWithinProviderRanges(details, offering);
        details.jobs().stream().filter(job -> !offering.jobs().contains(job)).findFirst().ifPresent(job -> {
            throw new JobNotOfferedException(job);
        });
        if (!offering.cities().contains(details.address().city())) {
            throw new CityNotCoveredException(details.address().city());
        }
    }

    private static void requireBookableStart(Instant start, PolicySnapshot policy, Instant now) {
        if (start.getEpochSecond() % QUARTER_HOUR_SECONDS != 0 || start.getNano() != 0) {
            throw new StartNotOnQuarterHourException(start);
        }
        Instant earliest = now.plus(policy.minimumNotice());
        if (start.isBefore(earliest)) {
            throw new StartTooSoonException(start, earliest);
        }
        Instant latest = now.plus(policy.bookingHorizon());
        if (start.isAfter(latest)) {
            throw new StartBeyondHorizonException(start, latest);
        }
    }

    private static void requireWithinProviderRanges(ReservationDetails details, OfferingSnapshot offering) {
        if (!offering.offersHours(details.hours())) {
            throw new HoursOutsideProviderRangeException(details.hours(), offering.minHours(), offering.maxHours());
        }
        if (!offering.offersHelpers(details.helpers())) {
            throw new HelpersOutsideProviderRangeException(details.helpers(), offering.minHelpers(), offering.maxHelpers());
        }
    }
}
