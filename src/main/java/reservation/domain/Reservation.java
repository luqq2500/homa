package reservation.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
import reservation.domain.exception.CompletionAlreadySubmittedException;
import reservation.domain.exception.CompletionDeadlineNotReachedException;
import reservation.domain.exception.CompletionNotSubmittedException;
import reservation.domain.exception.DisputeWindowStillOpenException;
import reservation.domain.exception.EvidenceRequiredException;
import reservation.domain.exception.FaceCheckAlreadyRecordedException;
import reservation.domain.exception.FlaggedReservationStartedException;
import reservation.domain.exception.HelperNotOnRosterException;
import reservation.domain.exception.HelpersOutsideProviderRangeException;
import reservation.domain.exception.HoursOutsideProviderRangeException;
import reservation.domain.exception.InvalidStatusTransitionException;
import reservation.domain.exception.JobEndsAfterProviderVerificationException;
import reservation.domain.exception.JobNotOfferedException;
import reservation.domain.exception.ListingPausedException;
import reservation.domain.exception.LocationNotCheckedException;
import reservation.domain.exception.MemberNotNamedException;
import reservation.domain.exception.NoShowWaitNotOverException;
import reservation.domain.exception.NotCheckedInException;
import reservation.domain.exception.PaymentNotSecuredException;
import reservation.domain.exception.ProviderMissedCheckInException;
import reservation.domain.exception.ReportAlreadyFiledException;
import reservation.domain.exception.ReportWindowClosedException;
import reservation.domain.exception.RequestExpiredException;
import reservation.domain.exception.RequestNotYetExpiredException;
import reservation.domain.exception.ReservationNotEditableException;
import reservation.domain.exception.StartBeyondHorizonException;
import reservation.domain.exception.StartNotOnQuarterHourException;
import reservation.domain.exception.StartTooSoonException;
import reservation.domain.exception.SupervisorNotOnRosterException;
import reservation.domain.exception.SupervisorRequiredException;
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
    private final List<FaceCheck> faceChecks = new ArrayList<>();
    private boolean faceCheckUnavailable;
    private Instant checkedInAt;
    private Ending ending;
    private Money securedPayment = Money.ZERO;
    private Instant noShowReportedAt;
    private Completion completion;
    private final Map<Party, Report> reports = new EnumMap<>(Party.class);

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

    /**
     * Submits a request for the provider to accept, with the offering and policy in force {@code now}. A
     * paused listing takes no new requests, and the job must end by the provider's re-verification due
     * date, {@code providerVerifiedUntil}.
     */
    public static Reservation request(
            String customerId,
            ReservationDetails details,
            OfferingSnapshot offering,
            PolicySnapshot policy,
            Optional<String> enquiryId,
            PaymentMethod paymentMethod,
            Instant providerVerifiedUntil,
            Instant now) {
        if (!offering.takesRequests()) {
            throw new ListingPausedException(offering.providerId());
        }
        requireFits(details, offering, policy, now);
        requireEndsByProviderVerification(details, providerVerifiedUntil);
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

    /**
     * Changes the request before the provider accepts it. The expiry still runs from the original request,
     * and the changed job must still end by the provider's re-verification due date.
     */
    public PriceChange edit(ReservationDetails newDetails, Instant providerVerifiedUntil, Instant now) {
        if (status != ReservationStatus.REQUESTED) {
            throw new ReservationNotEditableException(status);
        }
        requireNotExpired(now);
        requireFits(newDetails, offering, policy, now);
        requireEndsByProviderVerification(newDetails, providerVerifiedUntil);
        Money previous = price.total();
        details = newDetails;
        price = priceFor(newDetails);
        return new PriceChange(previous, price.total());
    }

    /**
     * The provider accepts the request. They can accept only a request whose payment is secured, and only
     * a job that ends by their re-verification due date as it is now, {@code providerVerifiedUntil};
     * passing a re-check moves it later.
     */
    public void accept(Instant providerVerifiedUntil, Instant now) {
        requireStatus(ReservationStatus.REQUESTED, "accepted");
        requireNotExpired(now);
        if (!isPaymentSecured()) {
            throw new PaymentNotSecuredException(id);
        }
        requireEndsByProviderVerification(details, providerVerifiedUntil);
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

    /**
     * Records the payment secured for the request so far: the amount authorised on the card, or paid by
     * FPX. The provider can accept only once it covers the total, so an edit that raises the total waits
     * for payment again.
     */
    public void confirmPayment(Money secured) {
        requireStatus(ReservationStatus.REQUESTED, "confirmed as paid");
        securedPayment = Objects.requireNonNull(secured);
    }

    /** Whether the payment secured so far covers the total. Until it does, the provider cannot accept. */
    public boolean isPaymentSecured() {
        return !securedPayment.isLessThan(price.total());
    }

    /**
     * The customer's payment failed when the request was made: the card was declined, or the FPX payment
     * failed or was abandoned. The request ends at once, so the provider is never left to answer a request
     * that is not paid for. Nothing was charged, so there is nothing to settle.
     */
    public void paymentFailed() {
        requireStatus(ReservationStatus.REQUESTED, "ended for a failed payment");
        status = ReservationStatus.CANCELLED;
        ending = Ending.PAYMENT_FAILED;
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
        requireNoCompletion();
        Optional<FineCharge> fine = providerCancellationFine(now);
        return new CancellationResult(endCancelled(Ending.PROVIDER_CANCELLED, Money.ZERO, now), fine);
    }

    /**
     * The cancellation terms of this booking for the customer, under the provider's tier: free until the
     * fee starts, the fee from then until the booked start, locked during the check-in period, and a
     * customer no-show fee after check-in. Cancelling while the request is still unanswered is always free.
     */
    public CancellationTerms cancellationTerms() {
        if (status != ReservationStatus.REQUESTED && status != ReservationStatus.RESERVED) {
            throw new InvalidStatusTransitionException(status, "given cancellation terms");
        }
        Money fee = tierFee();
        Instant freeUntil = fee.isZero() ? start() : feeWindowOpensAt();
        return new CancellationTerms(
                offering.cancellationTier(), freeUntil, fee, start(), checkInClosesAt(), customerNoShowFee());
    }

    // ---- Job day ----

    /**
     * Names who is coming: as many helpers as were booked, each a helper on the team, and at least one
     * supervisor from the team when the booking reaches the provider's supervisor rule. The team can change
     * until the booked start.
     */
    public void nameTeam(TeamAssignment newTeam, TeamRoster roster, Instant now) {
        requireStatus(ReservationStatus.RESERVED, "given a team");
        if (!now.isBefore(start())) {
            throw new TeamLockedException(start());
        }
        if (newTeam.helpers().size() != details.helpers()) {
            throw new TeamSizeMismatchException(newTeam.helpers().size(), details.helpers());
        }
        requireOnRoster(newTeam, roster);
        if (newTeam.supervisors().isEmpty() && offering.requiresSupervisor(details.helpers(), details.hours())) {
            throw new SupervisorRequiredException();
        }
        team = newTeam;
    }

    /**
     * The location check, the first step of check-in: the device is within the check-in radius, from the
     * earliest check-in to the end of the check-in period. Check-in completes with the first face check
     * passed, or when the face check service is unavailable.
     */
    public void checkIn(int distanceMetres, Instant now) {
        requireStatus(ReservationStatus.RESERVED, "checked in");
        if (team == null) {
            throw new TeamNotNamedException();
        }
        if (checkIn != null) {
            throw new AlreadyCheckedInException(checkIn.at());
        }
        requireInsideCheckInTimes(now);
        if (distanceMetres > policy.checkInRadiusMetres()) {
            throw new TooFarToCheckInException(distanceMetres, policy.checkInRadiusMetres());
        }
        checkIn = new CheckIn(now, distanceMetres);
    }

    /**
     * Records a named person's face check result from the KYC provider, after the location check. The
     * first pass completes check-in, which must happen by the end of the check-in period. A person whose
     * face check failed cannot work on the job; the KYC provider decides how many attempts they get.
     */
    public void recordFaceCheck(String memberId, FaceCheckResult result, Instant now) {
        requireLocationChecked();
        if (!team.names(memberId)) {
            throw new MemberNotNamedException(memberId);
        }
        if (faceCheckOf(memberId).isPresent()) {
            throw new FaceCheckAlreadyRecordedException(memberId);
        }
        requireCheckInStillOpen(now);
        faceChecks.add(new FaceCheck(memberId, result, now));
        if (result == FaceCheckResult.PASSED) {
            completeCheckIn(now);
        }
    }

    /**
     * The face check service is unavailable: check-in goes ahead on location only, marked "face check
     * unavailable" for the customer, and the provider is not fined. An outage never makes a no-show.
     */
    public void recordFaceCheckUnavailable(Instant now) {
        requireLocationChecked();
        requireCheckInStillOpen(now);
        faceCheckUnavailable = true;
        completeCheckIn(now);
    }

    /** Checked in: the location check passed, and one face check passed or the face check service was unavailable. */
    public boolean isCheckedIn() {
        return checkedInAt != null;
    }

    /**
     * Whether a named person can work on the job: they passed their face check, or the service was
     * unavailable and their face check had not already failed.
     */
    public boolean canWork(String memberId) {
        return isNamed(memberId) && (hasPassedFaceCheck(memberId) || coveredByOutage(memberId));
    }

    /**
     * Check-in not complete by the end of the check-in period, including when the location check passed but
     * no face check did: the customer is refunded in full and the provider is fined.
     */
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
        requireCheckedIn();
        requireNoCompletion();
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

    /** The provider marks the job completed, with photo or video evidence. There is no automatic completion. */
    public void submitCompletion(List<EvidenceRef> evidence, Instant now) {
        requireStatus(ReservationStatus.RESERVED, "completed");
        requireCheckedIn();
        requireNoCompletion();
        if (evidence.isEmpty()) {
            throw new EvidenceRequiredException();
        }
        completion = new Completion(now, evidence);
    }

    // ---- After the job ----

    /**
     * The dispute window ends the dispute window extension after the later of the booked end and the
     * completion. Until completion is submitted it has no end.
     */
    public Optional<Instant> disputeWindowEndsAt() {
        return Optional.ofNullable(completion)
                .map(done -> done.at().isAfter(end()) ? done.at() : end())
                .map(later -> later.plus(policy.disputeWindowExtension()));
    }

    /**
     * The customer or the provider files a report, from the booked start until the dispute window ends.
     * Each files at most one. A report sends the reservation to Homa.
     */
    public void fileReport(Party party, ReportType type, String reason, List<EvidenceRef> evidence, Instant now) {
        requireReportable();
        requireInsideDisputeWindow(now);
        if (reports.containsKey(party)) {
            throw new ReportAlreadyFiledException(party);
        }
        reports.put(party, new Report(party, type, reason, evidence, now));
        status = ReservationStatus.DISPUTED;
    }

    /** No completion by the completion deadline: Homa reviews the reservation. */
    public void missCompletionDeadline(Instant now) {
        requireStatus(ReservationStatus.RESERVED, "sent to Homa for a missed completion");
        requireNoCompletion();
        Instant deadline = end().plus(policy.completionDeadline());
        if (now.isBefore(deadline)) {
            throw new CompletionDeadlineNotReachedException(deadline);
        }
        status = ReservationStatus.DISPUTED;
    }

    /** The dispute window ended with no report: the job is completed and the payment is released. */
    /**
     * The KYC provider seriously flagged the provider's business, for example struck off or a sanctions
     * match. An accepted reservation whose job is not completed moves to Disputed at once: the team cannot
     * check in, and nobody files reports on it.
     */
    public void disputeForProviderFlag(Instant now) {
        requireStatus(ReservationStatus.RESERVED, "disputed for a flagged provider");
        requireNoCompletion();
        status = ReservationStatus.DISPUTED;
        ending = Ending.PROVIDER_FLAGGED;
    }

    /**
     * Homa's founders cancel a reservation disputed for a flagged provider, with a full refund and no fine.
     * It is also what happens at the booked start when nobody decided in time.
     */
    public Settlement cancelForProviderFlag(Instant now) {
        requireFlagged("cancelled for a flagged provider");
        status = ReservationStatus.CANCELLED;
        return settle(Money.ZERO, now);
    }

    /**
     * The KYC provider cleared the flag, and the founders restore the reservation before its booked start:
     * it is reserved again and goes ahead as booked.
     */
    public void restoreAfterFlagCleared(Instant now) {
        requireFlagged("restored");
        if (!now.isBefore(start())) {
            throw new FlaggedReservationStartedException(start());
        }
        status = ReservationStatus.RESERVED;
        ending = null;
    }

    /** Whether a flagged reservation is still undecided at its booked start, so Homa cancels it with a full refund. */
    public boolean isFlagCancellationDue(Instant now) {
        return status == ReservationStatus.DISPUTED && ending == Ending.PROVIDER_FLAGGED && !now.isBefore(start());
    }

    private void requireFlagged(String action) {
        if (status != ReservationStatus.DISPUTED || ending != Ending.PROVIDER_FLAGGED) {
            throw new InvalidStatusTransitionException(status, action);
        }
    }

    public Settlement closeDisputeWindow(Instant now) {
        requireStatus(ReservationStatus.RESERVED, "completed");
        Instant windowEnds = disputeWindowEndsAt().orElseThrow(CompletionNotSubmittedException::new);
        if (now.isBefore(windowEnds)) {
            throw new DisputeWindowStillOpenException(windowEnds);
        }
        status = ReservationStatus.COMPLETED;
        ending = Ending.COMPLETED;
        return settle(price.providerPrice(), now);
    }

    /**
     * Carries out Homa's decision on a disputed reservation. A job dispute ends completed, with the
     * provider price at stake. A challenged customer no-show ends cancelled, with the no-show fee at stake.
     */
    public Settlement resolve(CaseDecision decision, Instant now) {
        requireStatus(ReservationStatus.DISPUTED, "resolved");
        if (ending == Ending.PROVIDER_FLAGGED) {
            throw new InvalidStatusTransitionException(status, "resolved; a flagged provider's reservation is cancelled with a full refund");
        }
        boolean challengedNoShow = ending == Ending.CUSTOMER_NO_SHOW;
        Money atStake = challengedNoShow ? customerNoShowFee() : price.providerPrice();
        Money earning = decision.providerEarning(atStake);
        status = challengedNoShow ? ReservationStatus.CANCELLED : ReservationStatus.COMPLETED;
        ending = Ending.DECIDED_BY_HOMA;
        return settle(earning, now);
    }

    // ---- Reminders ----

    /**
     * Homa reminds the provider to name the team from the team reminder time before the booked start,
     * until the team is named. From the booked start the team is locked, so there is nothing to remind.
     */
    public boolean isTeamReminderDue(Instant now) {
        return status == ReservationStatus.RESERVED
                && team == null
                && !now.isBefore(start().minus(policy.teamReminder()))
                && now.isBefore(start());
    }

    /** Homa reminds the provider to submit completion from the completion reminder time after the booked end. */
    public boolean isCompletionReminderDue(Instant now) {
        return status == ReservationStatus.RESERVED
                && completion == null
                && !now.isBefore(end().plus(policy.completionReminder()));
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

    /** The location check; check-in is complete only with {@link #isCheckedIn()}. */
    public Optional<CheckIn> checkInRecord() {
        return Optional.ofNullable(checkIn);
    }

    /** When check-in completed, as the customer sees it. */
    public Optional<Instant> checkedInAt() {
        return Optional.ofNullable(checkedInAt);
    }

    /** Every face check attempt, with its result and time. */
    public List<FaceCheck> faceChecks() {
        return List.copyOf(faceChecks);
    }

    public boolean isFaceCheckUnavailable() {
        return faceCheckUnavailable;
    }

    public Optional<Ending> ending() {
        return Optional.ofNullable(ending);
    }

    public Optional<Completion> completion() {
        return Optional.ofNullable(completion);
    }

    public List<Report> reports() {
        return List.copyOf(reports.values());
    }

    // ---- Times ----

    private Instant start() {
        return details.start();
    }

    private Instant end() {
        return details.end();
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
        Instant from = checkedInAt.isAfter(start()) ? checkedInAt : start();
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
        requireNoCompletion();
        if (isCheckedIn()) {
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

    /** Before the booked start the provider's tier applies, exactly as {@link #cancellationTerms()} states it. */
    private Settlement cancelBeforeStartByCustomer(Instant now) {
        Money fee = cancellationTerms().feeAt(now);
        Ending why = fee.isZero() ? Ending.CUSTOMER_CANCELLED_FREE : Ending.CUSTOMER_CANCELLED_IN_FEE_WINDOW;
        return endCancelled(why, fee, now);
    }

    private Optional<FineCharge> providerCancellationFine(Instant now) {
        if (isCheckedIn() || !now.isBefore(start())) {
            return fine(FineReason.NO_SHOW, policy.providerNoShowFine());
        }
        if (!now.isBefore(feeWindowOpensAt())) {
            return fine(FineReason.LATE_CANCELLATION, policy.providerLateCancellationFine());
        }
        return Optional.empty();
    }

    private void requireCheckedIn() {
        if (!isCheckedIn()) {
            throw new NotCheckedInException();
        }
    }

    private void requireLocationChecked() {
        requireStatus(ReservationStatus.RESERVED, "face checked");
        if (checkIn == null) {
            throw new LocationNotCheckedException();
        }
    }

    /** Before check-in completes, its next step must still be inside the check-in period. */
    private void requireCheckInStillOpen(Instant now) {
        if (!isCheckedIn() && now.isAfter(checkInClosesAt())) {
            throw new CheckInPeriodOverException(checkInClosesAt());
        }
    }

    private void completeCheckIn(Instant now) {
        if (!isCheckedIn()) {
            checkedInAt = now;
        }
    }

    private Optional<FaceCheck> faceCheckOf(String memberId) {
        return faceChecks.stream().filter(check -> check.memberId().equals(memberId)).findFirst();
    }

    private boolean hasPassedFaceCheck(String memberId) {
        return faceCheckOf(memberId).filter(FaceCheck::passed).isPresent();
    }

    private boolean isNamed(String memberId) {
        return team != null && team.names(memberId);
    }

    /** During an outage, everyone named works except someone whose face check already failed. */
    private boolean coveredByOutage(String memberId) {
        return faceCheckUnavailable && faceCheckOf(memberId).isEmpty();
    }

    private void requireOnRoster(TeamAssignment newTeam, TeamRoster roster) {
        newTeam.helpers().stream().filter(helper -> !roster.helperIds().contains(helper)).findFirst().ifPresent(helper -> {
            throw new HelperNotOnRosterException(helper);
        });
        newTeam.supervisors().stream().filter(supervisor -> !roster.supervisorIds().contains(supervisor)).findFirst()
                .ifPresent(supervisor -> {
                    throw new SupervisorNotOnRosterException(supervisor);
                });
    }

    private void requireNoCompletion() {
        if (completion != null) {
            throw new CompletionAlreadySubmittedException(completion.at());
        }
    }

    /** A reserved booking, or one already disputed over the job, takes reports. A challenged no-show does not. */
    private void requireReportable() {
        boolean jobDispute = status == ReservationStatus.DISPUTED && ending == null;
        if (status != ReservationStatus.RESERVED && !jobDispute) {
            throw new InvalidStatusTransitionException(status, "reported");
        }
    }

    private void requireInsideDisputeWindow(Instant now) {
        if (now.isBefore(start())) {
            throw new ReportWindowClosedException("reports open at the booked start, " + start());
        }
        disputeWindowEndsAt().filter(windowEnds -> !now.isBefore(windowEnds)).ifPresent(windowEnds -> {
            throw new ReportWindowClosedException("the dispute window ended at " + windowEnds);
        });
    }

    private void requireNotCheckedIn() {
        if (isCheckedIn()) {
            throw new AlreadyCheckedInException(checkedInAt);
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

    /** A job ending exactly at the due date ends in time: the provider lapses only from that moment. */
    private static void requireEndsByProviderVerification(ReservationDetails details, Instant verifiedUntil) {
        if (details.end().isAfter(verifiedUntil)) {
            throw new JobEndsAfterProviderVerificationException(details.end(), verifiedUntil);
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
