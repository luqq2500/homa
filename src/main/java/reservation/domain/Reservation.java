package reservation.domain;

import static reservation.domain.ReservationStatus.CANCELLED;
import static reservation.domain.ReservationStatus.COMPLETED;
import static reservation.domain.ReservationStatus.DECLINED;
import static reservation.domain.ReservationStatus.DISPUTED;
import static reservation.domain.ReservationStatus.EXPIRED;
import static reservation.domain.ReservationStatus.REQUESTED;
import static reservation.domain.ReservationStatus.RESERVED;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import kernel.Money;
import kernel.TimeRange;
import reservation.domain.exception.AccessNotesTooLongException;
import reservation.domain.exception.CannotCancelDuringCheckInPeriodException;
import reservation.domain.exception.CityNotCoveredException;
import reservation.domain.exception.CompletionAlreadySubmittedException;
import reservation.domain.exception.CustomerNoShowNotReportableException;
import reservation.domain.exception.DecisionOutOfRangeException;
import reservation.domain.exception.InstantBookCannotBeDeclinedException;
import reservation.domain.exception.JobTeamInvalidException;
import reservation.domain.exception.JobTeamNotNamedException;
import reservation.domain.exception.NoShowReportWindowClosedException;
import reservation.domain.exception.NotCheckedInException;
import reservation.domain.exception.OutsideCheckInRadiusException;
import reservation.domain.exception.OutsideCheckInWindowException;
import reservation.domain.exception.PaymentNotSecuredException;
import reservation.domain.exception.PersonCannotWorkException;
import reservation.domain.exception.ProviderNotLiveException;
import reservation.domain.exception.ProviderNotVerifiedForJobException;
import reservation.domain.exception.ReportAlreadyFiledException;
import reservation.domain.exception.ReportWindowClosedException;
import reservation.domain.exception.RequestExpiredException;
import reservation.domain.exception.ReservationStatusException;
import reservation.domain.exception.TimeNotFreeException;

/**
 * A request and, once accepted, the reservation it becomes: from Requested, through the job day and
 * the dispute window, to its final status. It keeps the offering, the address and the Homa-set
 * values it was requested with.
 */
public final class Reservation {

    public static final int ACCESS_NOTES_LIMIT = 500;

    private final String id;
    private final String customerId;
    private final String customerName;
    private final Address address;
    private final ProviderTerms terms;
    private final HomaValues values;
    private final Instant requestedAt;
    private String accessNotes;
    private BookingDetails details;
    private PriceBreakdown price;
    private ReservationStatus status;
    private Ending ending;
    private boolean paymentSecured;
    private Instant acceptedAt;
    private JobTeam jobTeam;
    private boolean teamReminded;
    private Instant checkedInAt;
    private final Map<String, FaceCheck> faceChecks;
    private Completion completion;
    private boolean completionReminded;
    private Report customerReport;
    private Report providerReport;
    private DisputeCause disputeCause;
    private DisputeCause disputeCauseBeforeFlag;
    private Instant customerNoShowReportedAt;
    private boolean customerNoShowFeeReleased;
    private Settlement settlement;
    private Fine fine;

    private Reservation(ReservationState state) {
        this.id = state.id();
        this.customerId = state.customerId();
        this.customerName = state.customerName();
        this.address = state.address();
        this.accessNotes = state.accessNotes();
        this.terms = state.terms();
        this.values = state.values();
        this.details = state.details();
        this.price = state.price();
        this.status = state.status();
        this.ending = state.ending();
        this.requestedAt = state.requestedAt();
        this.paymentSecured = state.paymentSecured();
        this.acceptedAt = state.acceptedAt();
        this.jobTeam = state.jobTeam();
        this.teamReminded = state.teamReminded();
        this.checkedInAt = state.checkedInAt();
        this.faceChecks = new HashMap<>(state.faceChecks());
        this.completion = state.completion();
        this.completionReminded = state.completionReminded();
        this.customerReport = state.customerReport();
        this.providerReport = state.providerReport();
        this.disputeCause = state.disputeCause();
        this.disputeCauseBeforeFlag = state.disputeCauseBeforeFlag();
        this.customerNoShowReportedAt = state.customerNoShowReportedAt();
        this.customerNoShowFeeReleased = state.customerNoShowFeeReleased();
        this.settlement = state.settlement();
        this.fine = state.fine();
    }

    public static Reservation restore(ReservationState state) {
        return new Reservation(state);
    }

    public ReservationState state() {
        return new ReservationState(id, customerId, customerName, address, accessNotes, terms, values, details, price,
                status, ending, requestedAt, paymentSecured, acceptedAt, jobTeam, teamReminded, checkedInAt,
                faceChecks, completion, completionReminded, customerReport, providerReport, disputeCause,
                disputeCauseBeforeFlag, customerNoShowReportedAt, customerNoShowFeeReleased, settlement, fine);
    }

    // ---- Requesting ----

    /**
     * The customer requests a booking with a live provider who covers their city, at a time the
     * provider is free and verified for. The request copies the access notes from the customer's
     * account and waits for its payment to be secured.
     */
    public static Reservation request(Customer customer, ProviderTerms terms, ProviderStanding standing,
                                      ProviderSchedule schedule, BookingDetails details, HomaValues values,
                                      Instant now) {
        if (!terms.live()) {
            throw new ProviderNotLiveException(terms.providerId());
        }
        if (!terms.cities().contains(customer.address().city())) {
            throw new CityNotCoveredException(customer.address().city());
        }
        checkBookable(details, terms, standing, schedule, values, now);
        PriceBreakdown price = PriceBreakdown.of(terms, details, values.serviceFee());
        return new Reservation(new ReservationState(new ReservationId().generate(), customer.customerId(),
                customer.name(), customer.address(), customer.accessNotes(), terms, values, details, price,
                REQUESTED, null, now, false, null, null, false, null, Map.of(), null, false, null, null, null, null,
                null, false, null, null));
    }

    private static void checkBookable(BookingDetails details, ProviderTerms terms, ProviderStanding standing,
                                      ProviderSchedule schedule, HomaValues values, Instant now) {
        details.checkBookable(terms, values, now);
        checkVerifiedAndFree(details, standing, schedule);
    }

    private static void checkVerifiedAndFree(BookingDetails details, ProviderStanding standing,
                                             ProviderSchedule schedule) {
        if (!standing.isVerifiedFor(details.end())) {
            throw new ProviderNotVerifiedForJobException();
        }
        if (!schedule.isFreeFor(details.bookedTime())) {
            throw new TimeNotFreeException();
        }
    }

    /**
     * The customer edits the jobs, date and time, duration or helpers until the provider accepts.
     * The address cannot change, and the request keeps the offering it was made with. A changed
     * total must be secured again before the provider can accept. Editing does not restart the expiry.
     */
    public void edit(BookingDetails newDetails, ProviderStanding standing, ProviderSchedule schedule, Instant now) {
        require(status == REQUESTED, "be edited");
        requireNotExpired(now);
        checkBookable(newDetails, terms, standing, schedule, values, now);
        PriceBreakdown newPrice = PriceBreakdown.of(terms, newDetails, values.serviceFee());
        if (!newPrice.total().equals(price.total())) {
            paymentSecured = false;
        }
        details = newDetails;
        price = newPrice;
    }

    /**
     * A request expires the request expiry time after it was first submitted, and never later than
     * the response deadline before the booked start. At this moment the request has expired.
     */
    public Instant expiresAt() {
        Instant byExpiryTime = requestedAt.plus(values.requestExpiryTime());
        Instant byResponseDeadline = details.start().minus(values.responseDeadline());
        return byExpiryTime.isBefore(byResponseDeadline) ? byExpiryTime : byResponseDeadline;
    }

    // ---- The request's life ----

    /** The card is authorised or the upfront payment is paid: the provider now sees the request and can accept it. */
    public void securePayment() {
        require(status == REQUESTED, "have its payment secured");
        paymentSecured = true;
    }

    /** The card was declined, or the upfront payment failed or was abandoned: the request ends and nothing is charged. */
    public void failPayment() {
        require(status == REQUESTED, "have its payment fail");
        end(CANCELLED, Ending.PAYMENT_FAILED, Settlement.fullRefund());
    }

    /** The provider accepts, up to the moment the request expires. Only an accepted reservation holds the provider's time. */
    public void accept(ProviderStanding standing, ProviderSchedule schedule, Instant now) {
        requireAnswerable(now);
        checkVerifiedAndFree(details, standing, schedule);
        status = RESERVED;
        acceptedAt = now;
    }

    /**
     * Instant book: the request is accepted the moment its payment is secured, unless the time is
     * no longer free; then it ends and costs the customer nothing. Returns whether it was accepted.
     */
    public boolean acceptInstantly(ProviderStanding standing, ProviderSchedule schedule, Instant now) {
        requireAnswerable(now);
        if (!standing.isVerifiedFor(details.end()) || !schedule.isFreeFor(details.bookedTime())) {
            end(DECLINED, Ending.TIME_NO_LONGER_FREE, Settlement.fullRefund());
            return false;
        }
        status = RESERVED;
        acceptedAt = now;
        return true;
    }

    /** The provider declines. An instant-book request cannot be declined. */
    public void decline(Instant now) {
        requireAnswerable(now);
        if (terms.usesInstantBook()) {
            throw new InstantBookCannotBeDeclinedException();
        }
        end(DECLINED, Ending.DECLINED_BY_PROVIDER, Settlement.fullRefund());
    }

    /**
     * The provider accepted another booking: an open request whose time now clashes with it, or with
     * the buffer between jobs around it, is declined at once. Returns whether it was declined.
     */
    public boolean declineIfClashingWith(TimeRange acceptedJob, Duration buffer) {
        if (status != REQUESTED || !details.bookedTime().widenedBy(buffer).overlaps(acceptedJob)) {
            return false;
        }
        end(DECLINED, Ending.DECLINED_FOR_CLASH, Settlement.fullRefund());
        return true;
    }

    /** The provider did not respond in time. Returns whether the request expired just now. */
    public boolean expire(Instant now) {
        if (status != REQUESTED || now.isBefore(expiresAt())) {
            return false;
        }
        end(EXPIRED, Ending.EXPIRED, Settlement.fullRefund());
        return true;
    }

    private void requireAnswerable(Instant now) {
        require(status == REQUESTED, "be answered");
        if (!paymentSecured) {
            throw new PaymentNotSecuredException();
        }
        requireNotExpired(now);
    }

    private void requireNotExpired(Instant now) {
        if (!now.isBefore(expiresAt())) {
            throw new RequestExpiredException();
        }
    }

    // ---- Cancelling ----

    /**
     * The customer cancels. A request is cancelled for free. A reserved booking follows the
     * provider's cancellation policy until the booked start; after check-in it costs the customer
     * no-show fee; and during the check-in period it cannot be cancelled until the provider checks in.
     */
    public void cancelByCustomer(Instant now) {
        if (status == REQUESTED) {
            end(CANCELLED, Ending.CANCELLED_BY_CUSTOMER, Settlement.fullRefund());
            return;
        }
        requireReservedBeforeCompletion("be cancelled");
        end(CANCELLED, Ending.CANCELLED_BY_CUSTOMER, customerCancellationSettlement(now));
    }

    private Settlement customerCancellationSettlement(Instant now) {
        if (isCheckedIn()) {
            return new Settlement(customerNoShowFee(), PayoutKind.CUSTOMER_NO_SHOW_FEE);
        }
        if (!now.isBefore(details.start())) {
            throw new CannotCancelDuringCheckInPeriodException();
        }
        Money fee = price.cancellationFee(values.cancellationFeeOf(terms.cancellationTier()),
                values.customerNoShowFee());
        if (now.isBefore(feeWindowOpensAt()) || fee.isZero()) {
            return Settlement.fullRefund();
        }
        return new Settlement(fee, PayoutKind.CANCELLATION_FEE);
    }

    /**
     * The provider cancels: the customer is refunded in full. Before the fee window there is no
     * fine; within it, the late cancellation fine; after the booked start or after check-in, the
     * no-show fine.
     */
    public void cancelByProvider(Instant now) {
        requireReservedBeforeCompletion("be cancelled");
        fine = providerCancellationFine(now);
        end(CANCELLED, Ending.CANCELLED_BY_PROVIDER, Settlement.fullRefund());
    }

    private Fine providerCancellationFine(Instant now) {
        if (isCheckedIn() || !now.isBefore(details.start())) {
            return new Fine(price.fine(values.providerNoShowFine()), FineReason.NO_SHOW);
        }
        if (now.isBefore(feeWindowOpensAt())) {
            return null;
        }
        return new Fine(price.fine(values.providerLateCancellationFine()), FineReason.LATE_CANCELLATION);
    }

    /** The fee window opens this long before the booked start, for the customer's tiers and the provider's fine alike. */
    public Instant feeWindowOpensAt() {
        return details.start().minus(values.feeWindow());
    }

    public Money customerNoShowFee() {
        return price.customerNoShowFee(values.customerNoShowFee());
    }

    private void requireReservedBeforeCompletion(String action) {
        require(status == RESERVED, action);
        if (completion != null) {
            throw new CompletionAlreadySubmittedException();
        }
    }

    // ---- Job day ----

    /** Whether this booking reaches the provider's supervisor threshold, so at least one supervisor comes. */
    public boolean supervisorIncluded() {
        return terms.supervisorRule().includesSupervisorFor(details.helpers(), details.hours());
    }

    /** Before the booked start, the provider names who is coming, and can change the team until then. */
    public void nameJobTeam(Set<String> memberIds, TeamRoster roster, Instant now) {
        require(status == RESERVED, "have its team named");
        if (!now.isBefore(details.start())) {
            throw new JobTeamInvalidException("The team is named before the booked start");
        }
        jobTeam = JobTeam.name(memberIds, roster, details.helpers(), supervisorIncluded());
    }

    /** The team counts as named only while everyone who must come is on it. */
    public boolean isTeamNamed() {
        return jobTeam != null && jobTeam.isCompleteFor(details.helpers(), supervisorIncluded());
    }

    /**
     * A named person lapsed or was removed: they come off this job's team at once, and Homa reminds
     * the provider again. Returns whether they were named on it.
     */
    public boolean removeFromJobTeam(String memberId) {
        if (!isUpcomingOrUnderWay() || !isNamedOnJob(memberId)) {
            return false;
        }
        jobTeam = jobTeam.without(memberId);
        teamReminded = false;
        return true;
    }

    private boolean isUpcomingOrUnderWay() {
        return status == RESERVED || status == DISPUTED;
    }

    private boolean isNamedOnJob(String memberId) {
        return jobTeam != null && jobTeam.includes(memberId);
    }

    /** Homa reminds the provider at the team reminder time while the team is not named. Returns whether a reminder is due now. */
    public boolean remindTeamNaming(Instant now) {
        boolean due = awaitingTeam() && !teamReminded
                && !now.isBefore(details.start().minus(values.teamReminderTime()));
        if (due) {
            teamReminded = true;
        }
        return due;
    }

    private boolean awaitingTeam() {
        return status == RESERVED && !isTeamNamed();
    }

    /**
     * The provider checks in at the door: the device is within the check-in radius, and a named
     * person takes the face check. Check-in counts once the first named person passes, or goes ahead
     * on location only when the face check service is unavailable.
     */
    public void checkIn(String memberId, FaceCheckResult result, boolean withinRadius, Instant now) {
        requireReadyForCheckIn();
        requireInsideCheckInWindow(now);
        if (!withinRadius) {
            throw new OutsideCheckInRadiusException();
        }
        recordFaceCheck(memberId, result, now);
        if (result != FaceCheckResult.FAILED) {
            checkedInAt = now;
        }
    }

    private void requireReadyForCheckIn() {
        require(status == RESERVED && !isCheckedIn(), "be checked in");
        if (!isTeamNamed()) {
            throw new JobTeamNotNamedException();
        }
    }

    /** From the earliest check-in time up to and including the booked start plus the check-in period. */
    private void requireInsideCheckInWindow(Instant now) {
        if (now.isBefore(details.start().minus(values.earliestCheckIn())) || now.isAfter(checkInClosesAt())) {
            throw new OutsideCheckInWindowException();
        }
    }

    /** After check-in, each other named person passes the face check before working. */
    public void faceCheck(String memberId, FaceCheckResult result, Instant now) {
        if (!isCheckedIn()) {
            throw new NotCheckedInException();
        }
        recordFaceCheck(memberId, result, now);
    }

    private void recordFaceCheck(String memberId, FaceCheckResult result, Instant now) {
        if (!jobTeam.includes(memberId)) {
            throw new PersonCannotWorkException(memberId);
        }
        faceChecks.put(memberId, new FaceCheck(memberId, result, now));
    }

    /** A named person can work unless they failed the face check. */
    public boolean canWork(String memberId) {
        boolean failed = Optional.ofNullable(faceChecks.get(memberId))
                .map(check -> check.result() == FaceCheckResult.FAILED).orElse(false);
        return jobTeam != null && jobTeam.includes(memberId) && !failed;
    }

    public boolean isCheckedIn() {
        return checkedInAt != null;
    }

    /** The last moment the provider can check in: the booked start plus the check-in period. */
    public Instant checkInClosesAt() {
        return details.start().plus(values.checkInPeriod());
    }

    /**
     * No check-in by the end of the check-in period is a provider no-show: the reservation is
     * cancelled, the customer is refunded in full and the provider is fined. Returns whether it ended just now.
     */
    public boolean endAsProviderNoShow(Instant now) {
        if (status != RESERVED || isCheckedIn() || !now.isAfter(checkInClosesAt())) {
            return false;
        }
        fine = new Fine(price.fine(values.providerNoShowFine()), FineReason.NO_SHOW);
        end(CANCELLED, Ending.PROVIDER_NO_SHOW, Settlement.fullRefund());
        return true;
    }

    /**
     * The provider checked in and the customer is not there: after the customer no-show wait, and up
     * to and including the booked end, the provider reports a customer no-show and is due the
     * customer no-show fee. The fee is held until the no-show report window ends.
     */
    public void reportCustomerNoShow(Instant now) {
        requireReservedBeforeCompletion("be reported as a customer no-show");
        if (!isCheckedIn()) {
            throw new NotCheckedInException();
        }
        Instant waitFrom = checkedInAt.isAfter(details.start()) ? checkedInAt : details.start();
        if (now.isBefore(waitFrom.plus(values.customerNoShowWait()))) {
            throw new CustomerNoShowNotReportableException("The customer no-show wait has not passed");
        }
        if (now.isAfter(details.end())) {
            throw new CustomerNoShowNotReportableException("A customer no-show is reported by the booked end");
        }
        customerNoShowReportedAt = now;
        end(CANCELLED, Ending.CUSTOMER_NO_SHOW, new Settlement(customerNoShowFee(), PayoutKind.CUSTOMER_NO_SHOW_FEE));
    }

    /** The no-show report window closes at the moment the fee is released. */
    public Optional<Instant> noShowReportWindowClosesAt() {
        return Optional.ofNullable(customerNoShowReportedAt).map(at -> at.plus(values.noShowReportWindow()));
    }

    /** The customer reports a wrongful no-show within the no-show report window; Homa then investigates. */
    public void challengeNoShow(Instant now) {
        require(awaitingNoShowFeeRelease(), "have a no-show challenged");
        if (!now.isBefore(noShowReportWindowClosesAt().orElseThrow())) {
            throw new NoShowReportWindowClosedException();
        }
        status = DISPUTED;
        disputeCause = DisputeCause.NO_SHOW_CHALLENGE;
        settlement = null;
    }

    /**
     * The no-show report window ended without the customer reporting it: the fee is released and the
     * customer's refund of the rest is paid at the same time. Returns whether it was released just now.
     */
    public boolean releaseCustomerNoShowFee(Instant now) {
        if (!awaitingNoShowFeeRelease() || now.isBefore(noShowReportWindowClosesAt().orElseThrow())) {
            return false;
        }
        customerNoShowFeeReleased = true;
        return true;
    }

    private boolean awaitingNoShowFeeRelease() {
        return status == CANCELLED && ending == Ending.CUSTOMER_NO_SHOW && !customerNoShowFeeReleased;
    }

    // ---- Completion and disputes ----

    /**
     * The provider marks the job completed, with evidence. There is no automatic completion. It can
     * still be submitted while the reservation is Disputed over the job; the status stays Disputed.
     */
    public void submitCompletion(List<String> evidence, Instant now) {
        require(isCheckedIn() && (status == RESERVED || disputedOverTheJob()), "have completion submitted");
        if (completion != null) {
            throw new CompletionAlreadySubmittedException();
        }
        completion = new Completion(evidence, now);
    }

    /** Homa reminds the provider to submit completion after the completion reminder time. Returns whether a reminder is due now. */
    public boolean remindCompletion(Instant now) {
        boolean due = awaitingCompletion() && !completionReminded
                && !now.isBefore(details.end().plus(values.completionReminderTime()));
        if (due) {
            completionReminded = true;
        }
        return due;
    }

    /** The job is under way or over, and the provider has not submitted completion yet. */
    private boolean awaitingCompletion() {
        return status == RESERVED && isCheckedIn() && completion == null;
    }

    /**
     * The dispute window ends the dispute window extension after the later of the booked end and the
     * moment the provider submits completion. Until completion is submitted it has no end.
     */
    public Optional<Instant> disputeWindowEndsAt() {
        return Optional.ofNullable(completion).map(Completion::submittedAt)
                .map(submittedAt -> submittedAt.isAfter(details.end()) ? submittedAt : details.end())
                .map(later -> later.plus(values.disputeWindowExtension()));
    }

    /**
     * The customer or the provider files their one report, from the booked start until the moment
     * the dispute window ends. It moves the reservation to Disputed, where the other party can still
     * file theirs.
     */
    public void fileReport(Report report) {
        require(status == RESERVED || disputedOverTheJob(), "take a report");
        requireReportWindowOpen(report.filedAt());
        keep(report);
        if (status == RESERVED) {
            status = DISPUTED;
            disputeCause = DisputeCause.REPORT;
        }
    }

    private void requireReportWindowOpen(Instant filedAt) {
        if (filedAt.isBefore(details.start()) || disputeWindowEnded(filedAt)) {
            throw new ReportWindowClosedException();
        }
    }

    private void keep(Report report) {
        if (report.party() == ReportingParty.CUSTOMER) {
            requireNoReportYet(customerReport);
            customerReport = report;
        } else {
            requireNoReportYet(providerReport);
            providerReport = report;
        }
    }

    private static void requireNoReportYet(Report filed) {
        if (filed != null) {
            throw new ReportAlreadyFiledException();
        }
    }

    private boolean disputeWindowEnded(Instant now) {
        return disputeWindowEndsAt().map(endsAt -> !now.isBefore(endsAt)).orElse(false);
    }

    /** Disputed after a report or a missed completion deadline, not over a no-show challenge or a serious flag. */
    private boolean disputedOverTheJob() {
        return status == DISPUTED
                && (disputeCause == DisputeCause.REPORT || disputeCause == DisputeCause.MISSED_COMPLETION_DEADLINE);
    }

    /**
     * The provider has still not submitted completion by the completion deadline: the reservation
     * moves to Disputed and Homa reviews it. Returns whether it moved just now.
     */
    public boolean missCompletionDeadline(Instant now) {
        boolean missed = awaitingCompletion() && !now.isBefore(details.end().plus(values.completionDeadline()));
        if (missed) {
            status = DISPUTED;
            disputeCause = DisputeCause.MISSED_COMPLETION_DEADLINE;
        }
        return missed;
    }

    /**
     * The dispute window ended with no report: the reservation is Completed and the payment is
     * released to the provider. Returns whether it completed just now.
     */
    public boolean completeAfterDisputeWindow(Instant now) {
        if (status != RESERVED || !disputeWindowEnded(now)) {
            return false;
        }
        end(COMPLETED, Ending.DISPUTE_WINDOW_ENDED, new Settlement(price.providerPrice(), PayoutKind.JOB));
        return true;
    }

    /** What Homa's decision is about: the provider price for a job, or the customer no-show fee for a challenged no-show. */
    public Money atStake() {
        return disputeCause == DisputeCause.NO_SHOW_CHALLENGE ? customerNoShowFee() : price.providerPrice();
    }

    /**
     * Homa decides a review: the provider receives any amount from RM0 up to what was at stake. A
     * job becomes Completed; a challenged customer no-show ends Cancelled.
     */
    public void decide(Money providerReceives) {
        require(status == DISPUTED && disputeCause != DisputeCause.SERIOUS_FLAG, "be decided");
        if (providerReceives.isMoreThan(atStake())) {
            throw new DecisionOutOfRangeException(atStake());
        }
        if (disputeCause == DisputeCause.NO_SHOW_CHALLENGE) {
            customerNoShowFeeReleased = true;
            end(CANCELLED, Ending.DECIDED_BY_HOMA, new Settlement(providerReceives, PayoutKind.CUSTOMER_NO_SHOW_FEE));
        } else {
            end(COMPLETED, Ending.DECIDED_BY_HOMA, new Settlement(providerReceives, PayoutKind.JOB));
        }
    }

    // ---- A seriously flagged business ----

    /**
     * The provider's business was seriously flagged: an accepted reservation whose completion has
     * not been submitted moves to Disputed for the flag, keeping any report with it. Returns whether it moved.
     */
    public boolean disputeForSeriousFlag() {
        if (completion != null || !(status == RESERVED || disputedOverTheJob())) {
            return false;
        }
        disputeCauseBeforeFlag = disputeCause;
        status = DISPUTED;
        disputeCause = DisputeCause.SERIOUS_FLAG;
        return true;
    }

    public boolean isDisputedForSeriousFlag() {
        return status == DISPUTED && disputeCause == DisputeCause.SERIOUS_FLAG;
    }

    /** The normal outcome: cancelled with a full refund, with no fine and no cancellation fee. */
    public void cancelForSeriousFlag() {
        require(isDisputedForSeriousFlag(), "be cancelled for a serious flag");
        end(CANCELLED, Ending.CANCELLED_FOR_SERIOUS_FLAG, Settlement.fullRefund());
    }

    /** The flag was cleared and the booked start has not come: the reservation goes ahead as booked. */
    public void restoreAfterFlagCleared(Instant now) {
        require(isDisputedForSeriousFlag() && now.isBefore(details.start()), "be restored");
        disputeCause = disputeCauseBeforeFlag;
        disputeCauseBeforeFlag = null;
        status = disputeCause == null ? RESERVED : DISPUTED;
    }

    /** Nobody decided by the booked start: Homa cancels with a full refund. Returns whether it was cancelled just now. */
    public boolean cancelUndecidedFlagAtStart(Instant now) {
        if (!isDisputedForSeriousFlag() || now.isBefore(details.start())) {
            return false;
        }
        cancelForSeriousFlag();
        return true;
    }

    // ---- Access notes ----

    /** The customer can change a reservation's access notes until the job is completed. */
    public void changeAccessNotes(String notes) {
        require((status == REQUESTED || isUpcomingOrUnderWay()) && completion == null,
                "have its access notes changed");
        String checked = Optional.ofNullable(notes).orElse("");
        if (checked.length() > ACCESS_NOTES_LIMIT) {
            throw new AccessNotesTooLongException(ACCESS_NOTES_LIMIT);
        }
        accessNotes = checked;
    }

    private void end(ReservationStatus finalStatus, Ending how, Settlement split) {
        status = finalStatus;
        ending = how;
        settlement = split;
    }

    private void require(boolean allowed, String action) {
        if (!allowed) {
            throw new ReservationStatusException(status.name(), action);
        }
    }

    // ---- What others read ----

    /** An accepted reservation that has not ended holds the provider's time. */
    public boolean holdsProvidersTime() {
        return isUpcomingOrUnderWay();
    }

    public boolean isRequested() {
        return status == REQUESTED;
    }

    public String id() {
        return id;
    }

    public String customerId() {
        return customerId;
    }

    public String providerId() {
        return terms.providerId();
    }

    public ReservationStatus status() {
        return status;
    }

    public BookingDetails details() {
        return details;
    }

    public PriceBreakdown price() {
        return price;
    }

    public boolean isPaymentSecured() {
        return paymentSecured;
    }

    public Optional<Ending> ending() {
        return Optional.ofNullable(ending);
    }

    public Optional<Settlement> settlement() {
        return Optional.ofNullable(settlement);
    }

    public Optional<Fine> fine() {
        return Optional.ofNullable(fine);
    }

    public Optional<JobTeam> jobTeam() {
        return Optional.ofNullable(jobTeam);
    }

    public Optional<DisputeCause> disputeCause() {
        return Optional.ofNullable(disputeCause);
    }
}
