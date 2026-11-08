package reservation.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import kernel.Money;
import kernel.Percentage;
import reservation.domain.exception.AlreadyCheckedInException;
import reservation.domain.exception.CancellationLockedDuringCheckInPeriodException;
import reservation.domain.exception.CheckInPeriodNotOverException;
import reservation.domain.exception.CheckInPeriodOverException;
import reservation.domain.exception.CheckInTooEarlyException;
import reservation.domain.exception.CityNotCoveredException;
import reservation.domain.exception.HelperNotOnRosterException;
import reservation.domain.exception.HelpersOutsideProviderRangeException;
import reservation.domain.exception.HoursOutsideProviderRangeException;
import reservation.domain.exception.InvalidStatusTransitionException;
import reservation.domain.exception.JobNotOfferedException;
import reservation.domain.exception.NoShowWaitNotOverException;
import reservation.domain.exception.NotCheckedInException;
import reservation.domain.exception.ProviderMissedCheckInException;
import reservation.domain.exception.ReportWindowClosedException;
import reservation.domain.exception.RequestExpiredException;
import reservation.domain.exception.RequestNotYetExpiredException;
import reservation.domain.exception.ReservationNotEditableException;
import reservation.domain.exception.StartBeyondHorizonException;
import reservation.domain.exception.StartNotOnQuarterHourException;
import reservation.domain.exception.StartTooSoonException;
import reservation.domain.exception.TeamLockedException;
import reservation.domain.exception.TeamNotNamedException;
import reservation.domain.exception.TeamSizeMismatchException;
import reservation.domain.exception.TooFarToCheckInException;

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
    private TeamAssignment team;
    private CheckIn checkIn;
    private Ending ending;
    private Instant noShowReportedAt;

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

    // ---- Cancelling ----

    /**
     * The customer cancels. A request is free on every tier. A reserved booking follows the provider's
     * tier until the booked start. During the check-in period the customer cannot cancel until the
     * provider checks in, and after check-in a cancellation counts as a customer no-show.
     */
    public Settlement cancelByCustomer(Instant now) {
        return switch (status) {
            case REQUESTED -> endCancelled(Ending.CUSTOMER_CANCELLED_REQUEST, Money.ZERO, now);
            case RESERVED -> cancelReservedByCustomer(now);
            default -> throw new InvalidStatusTransitionException(status, "cancelled by the customer");
        };
    }

    /**
     * The provider cancels a reserved booking. The customer is refunded in full. The fine depends only on
     * timing: none before the fee window, the late cancellation fine inside it, and the no-show fine from
     * the booked start or after check-in.
     */
    public CancellationResult cancelByProvider(Instant now) {
        requireStatus(ReservationStatus.RESERVED, "cancelled by the provider");
        Optional<FineCharge> fine = providerCancellationFine(now);
        return new CancellationResult(endCancelled(Ending.PROVIDER_CANCELLED, Money.ZERO, now), fine);
    }

    // ---- Job day ----

    /** Names who is coming. The team can change until the booked start. */
    public void nameTeam(TeamAssignment newTeam, TeamRoster roster, Instant now) {
        requireStatus(ReservationStatus.RESERVED, "given a team");
        if (!now.isBefore(start())) {
            throw new TeamLockedException(start());
        }
        if (newTeam.helpers().size() != details.helpers()) {
            throw new TeamSizeMismatchException(newTeam.helpers().size(), details.helpers());
        }
        newTeam.helpers().stream().filter(helper -> !roster.includes(helper)).findFirst().ifPresent(helper -> {
            throw new HelperNotOnRosterException(helper);
        });
        team = newTeam;
    }

    /** The lead checks in at the customer's location, from the earliest check-in to the end of the check-in period. */
    public void checkIn(int distanceMetres, Instant now) {
        requireStatus(ReservationStatus.RESERVED, "checked in");
        if (team == null) {
            throw new TeamNotNamedException();
        }
        requireNotCheckedIn();
        requireInsideCheckInTimes(now);
        if (distanceMetres > policy.checkInRadiusMetres()) {
            throw new TooFarToCheckInException(distanceMetres, policy.checkInRadiusMetres());
        }
        checkIn = new CheckIn(now, distanceMetres);
    }

    /** No check-in by the end of the check-in period: the customer is refunded in full and the provider is fined. */
    public CancellationResult markProviderNoShow(Instant now) {
        requireStatus(ReservationStatus.RESERVED, "marked a provider no-show");
        requireNotCheckedIn();
        if (!now.isAfter(checkInClosesAt())) {
            throw new CheckInPeriodNotOverException(checkInClosesAt());
        }
        Settlement refund = endCancelled(Ending.PROVIDER_NO_SHOW, Money.ZERO, now);
        return new CancellationResult(refund, fine(FineReason.NO_SHOW, policy.providerNoShowFine()));
    }

    /**
     * The provider checked in and the customer is not there after the customer no-show wait. The provider
     * earns the customer no-show fee, released when the no-show report window ends unless the customer
     * challenges it.
     */
    public Settlement reportCustomerNoShow(Instant now) {
        requireStatus(ReservationStatus.RESERVED, "reported as a customer no-show");
        if (checkIn == null) {
            throw new NotCheckedInException();
        }
        if (now.isBefore(noShowWaitEndsAt())) {
            throw new NoShowWaitNotOverException(noShowWaitEndsAt());
        }
        noShowReportedAt = now;
        status = ReservationStatus.CANCELLED;
        ending = Ending.CUSTOMER_NO_SHOW;
        return settle(customerNoShowFee(), now.plus(policy.noShowReportWindow()));
    }

    /** The customer reports a wrongful no-show within the no-show report window. Homa then decides. */
    public void challengeNoShow(Instant now) {
        if (ending != Ending.CUSTOMER_NO_SHOW || status != ReservationStatus.CANCELLED) {
            throw new InvalidStatusTransitionException(status, "challenged as a no-show");
        }
        Instant windowEnds = noShowReportedAt.plus(policy.noShowReportWindow());
        if (!now.isBefore(windowEnds)) {
            throw new ReportWindowClosedException("the no-show report window ended at " + windowEnds);
        }
        status = ReservationStatus.DISPUTED;
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

    public Optional<TeamAssignment> team() {
        return Optional.ofNullable(team);
    }

    public Optional<CheckIn> checkInRecord() {
        return Optional.ofNullable(checkIn);
    }

    public Optional<Ending> ending() {
        return Optional.ofNullable(ending);
    }

    // ---- Times ----

    private Instant start() {
        return details.start();
    }

    private Instant feeWindowOpensAt() {
        return start().minus(policy.feeWindow());
    }

    private Instant checkInOpensAt() {
        return start().minus(policy.earliestCheckIn());
    }

    private Instant checkInClosesAt() {
        return start().plus(policy.checkInPeriod());
    }

    /** The wait runs from the later of the booked start and the check-in. */
    private Instant noShowWaitEndsAt() {
        Instant from = checkIn.at().isAfter(start()) ? checkIn.at() : start();
        return from.plus(policy.customerNoShowWait());
    }

    // ---- Money ----

    private Money tierFee() {
        return policy.cancellationFee(offering.cancellationTier()).of(price.hoursFee());
    }

    /** The travel fee plus a share of the hours fee. */
    private Money customerNoShowFee() {
        return price.travelFee().plus(policy.customerNoShowFee().of(price.hoursFee()));
    }

    private Optional<FineCharge> fine(FineReason reason, Percentage share) {
        return Optional.of(new FineCharge(providerId(), id, share.of(price.hoursFee()), reason));
    }

    private Settlement settle(Money providerEarning, Instant releaseAt) {
        return Settlement.of(id, ending, price.total(), providerEarning, policy.serviceFee(), releaseAt);
    }

    private Settlement endCancelled(Ending why, Money providerEarning, Instant now) {
        status = ReservationStatus.CANCELLED;
        ending = why;
        return settle(providerEarning, now);
    }

    // ---- Rules ----

    private Settlement cancelReservedByCustomer(Instant now) {
        if (checkIn != null) {
            return endCancelled(Ending.CUSTOMER_CANCELLED_AFTER_CHECK_IN, customerNoShowFee(), now);
        }
        if (now.isBefore(start())) {
            return cancelBeforeStartByCustomer(now);
        }
        if (!now.isAfter(checkInClosesAt())) {
            throw new CancellationLockedDuringCheckInPeriodException();
        }
        throw new ProviderMissedCheckInException();
    }

    /** Before the booked start the provider's tier applies: its fee inside the fee window, nothing before it. */
    private Settlement cancelBeforeStartByCustomer(Instant now) {
        Money fee = now.isBefore(feeWindowOpensAt()) ? Money.ZERO : tierFee();
        Ending why = fee.isZero() ? Ending.CUSTOMER_CANCELLED_FREE : Ending.CUSTOMER_CANCELLED_IN_FEE_WINDOW;
        return endCancelled(why, fee, now);
    }

    private Optional<FineCharge> providerCancellationFine(Instant now) {
        if (checkIn != null || !now.isBefore(start())) {
            return fine(FineReason.NO_SHOW, policy.providerNoShowFine());
        }
        if (!now.isBefore(feeWindowOpensAt())) {
            return fine(FineReason.LATE_CANCELLATION, policy.providerLateCancellationFine());
        }
        return Optional.empty();
    }

    private void requireNotCheckedIn() {
        if (checkIn != null) {
            throw new AlreadyCheckedInException(checkIn.at());
        }
    }

    private void requireInsideCheckInTimes(Instant now) {
        if (now.isBefore(checkInOpensAt())) {
            throw new CheckInTooEarlyException(checkInOpensAt());
        }
        if (now.isAfter(checkInClosesAt())) {
            throw new CheckInPeriodOverException(checkInClosesAt());
        }
    }

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
